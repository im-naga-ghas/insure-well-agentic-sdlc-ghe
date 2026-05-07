import json
import os
import sqlite3
import time
from datetime import datetime, timezone

from flask import (Flask, g, jsonify, redirect, render_template,
                   request, send_from_directory, url_for)
from werkzeug.utils import secure_filename

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 5 * 1024 * 1024  # 5 MB

BASE_DIR   = os.path.dirname(os.path.abspath(__file__))
DATA_DIR   = os.path.join(BASE_DIR, 'data')
DB_PATH    = os.path.join(DATA_DIR, 'insurewell.db')
DATA_FILE  = os.path.join(DATA_DIR, 'store.json')
UPLOAD_DIR = os.path.join(BASE_DIR, 'uploads')
ALLOWED    = {'pdf', 'jpg', 'jpeg', 'png'}

# ── Database ───────────────────────────────────────────────────────────────────

def get_db():
    """Return the per-request SQLite connection (stored on Flask's g object)."""
    if 'db' not in g:
        g.db = sqlite3.connect(DB_PATH, detect_types=sqlite3.PARSE_DECLTYPES)
        g.db.row_factory = sqlite3.Row
        g.db.execute('PRAGMA journal_mode=WAL')
        g.db.execute('PRAGMA foreign_keys=ON')
    return g.db


@app.teardown_appcontext
def close_db(exc=None):
    db = g.pop('db', None)
    if db is not None:
        db.close()


def init_db():
    """Create tables and seed sample data if the database is new."""
    os.makedirs(DATA_DIR, exist_ok=True)
    os.makedirs(UPLOAD_DIR, exist_ok=True)

    db = sqlite3.connect(DB_PATH)
    db.row_factory = sqlite3.Row
    db.execute('PRAGMA foreign_keys=ON')

    db.executescript('''
        CREATE TABLE IF NOT EXISTS policies (
            id              TEXT PRIMARY KEY,
            holder_name     TEXT NOT NULL,
            plan_name       TEXT NOT NULL,
            coverage_amount REAL NOT NULL,
            status          TEXT NOT NULL DEFAULT 'active',
            start_date      TEXT NOT NULL,
            end_date        TEXT NOT NULL,
            created_at      TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS claims (
            id              TEXT PRIMARY KEY,
            policy_id       TEXT NOT NULL REFERENCES policies(id),
            amount          REAL NOT NULL,
            description     TEXT NOT NULL,
            status          TEXT NOT NULL DEFAULT 'Pending',
            file_name       TEXT,
            stored_file_name TEXT,
            submitted_at    TEXT NOT NULL,
            updated_at      TEXT NOT NULL
        );
    ''')

    columns = {r['name'] for r in db.execute("PRAGMA table_info('claims')").fetchall()}
    if 'stored_file_name' not in columns:
        db.execute('ALTER TABLE claims ADD COLUMN stored_file_name TEXT')

    # Seed only when tables are empty
    if db.execute('SELECT COUNT(*) FROM policies').fetchone()[0] == 0:
        db.executemany(
            '''INSERT INTO policies
               (id, holder_name, plan_name, coverage_amount, status, start_date, end_date, created_at)
               VALUES (:id, :holder_name, :plan_name, :coverage_amount, :status,
                       :start_date, :end_date, :created_at)''',
            [
                {
                    'id': 'POL-2024-001', 'holder_name': 'Alex Johnson',
                    'plan_name': 'InsureWell Premium Health Plan',
                    'coverage_amount': 250000, 'status': 'active',
                    'start_date': '2024-01-01', 'end_date': '2026-12-31',
                    'created_at': '2024-01-01T00:00:00.000Z',
                },
                {
                    'id': 'POL-2024-002', 'holder_name': 'Maria Garcia',
                    'plan_name': 'InsureWell Essential Care Plan',
                    'coverage_amount': 150000, 'status': 'active',
                    'start_date': '2024-03-15', 'end_date': '2026-03-14',
                    'created_at': '2024-03-15T00:00:00.000Z',
                },
                {
                    'id': 'POL-2023-009', 'holder_name': 'David Chen',
                    'plan_name': 'InsureWell Family Plus Plan',
                    'coverage_amount': 500000, 'status': 'inactive',
                    'start_date': '2023-06-01', 'end_date': '2024-05-31',
                    'created_at': '2023-06-01T00:00:00.000Z',
                },
            ],
        )
        db.executemany(
            '''INSERT INTO claims
               (id, policy_id, amount, description, status, file_name, stored_file_name, submitted_at, updated_at)
                VALUES (:id, :policy_id, :amount, :description, :status,
                        :file_name, :stored_file_name, :submitted_at, :updated_at)''',
            [
                {
                    'id': 'CLM-1001', 'policy_id': 'POL-2024-001', 'amount': 3200,
                    'description': 'Emergency room visit – chest pain evaluation',
                    'status': 'Approved', 'file_name': 'er_receipt_jan.pdf',
                    'stored_file_name': None,
                    'submitted_at': '2024-02-10T09:15:00.000Z',
                    'updated_at':   '2024-02-14T11:00:00.000Z',
                },
                {
                    'id': 'CLM-1002', 'policy_id': 'POL-2024-001', 'amount': 850,
                    'description': 'Specialist consultation – orthopedics follow-up',
                    'status': 'Approved', 'file_name': 'ortho_invoice.pdf',
                    'stored_file_name': None,
                    'submitted_at': '2024-03-05T14:30:00.000Z',
                    'updated_at':   '2024-03-09T10:20:00.000Z',
                },
                {
                    'id': 'CLM-1003', 'policy_id': 'POL-2024-001', 'amount': 12500,
                    'description': 'Knee surgery – arthroscopic procedure',
                    'status': 'Pending', 'file_name': 'surgery_discharge_summary.pdf',
                    'stored_file_name': None,
                    'submitted_at': '2024-04-18T08:00:00.000Z',
                    'updated_at':   '2024-04-18T08:00:00.000Z',
                },
                {
                    'id': 'CLM-1004', 'policy_id': 'POL-2024-001', 'amount': 420,
                    'description': 'Prescription medications – 3-month supply',
                    'status': 'Rejected', 'file_name': None,
                    'stored_file_name': None,
                    'submitted_at': '2024-05-02T16:45:00.000Z',
                    'updated_at':   '2024-05-07T09:30:00.000Z',
                },
                {
                    'id': 'CLM-1005', 'policy_id': 'POL-2024-001', 'amount': 1750,
                    'description': 'MRI scan – lower back pain diagnosis',
                    'status': 'Pending', 'file_name': 'mri_report.jpg',
                    'stored_file_name': None,
                    'submitted_at': '2024-06-20T11:00:00.000Z',
                    'updated_at':   '2024-06-20T11:00:00.000Z',
                },
                {
                    'id': 'CLM-2001', 'policy_id': 'POL-2024-002', 'amount': 600,
                    'description': 'Annual physical exam and blood work panel',
                    'status': 'Approved', 'file_name': 'lab_results.pdf',
                    'stored_file_name': None,
                    'submitted_at': '2024-04-01T10:00:00.000Z',
                    'updated_at':   '2024-04-05T14:00:00.000Z',
                },
                {
                    'id': 'CLM-2002', 'policy_id': 'POL-2024-002', 'amount': 2300,
                    'description': 'Dental surgery – wisdom tooth extraction',
                    'status': 'Pending', 'file_name': 'dental_xray.png',
                    'stored_file_name': None,
                    'submitted_at': '2024-07-10T09:00:00.000Z',
                    'updated_at':   '2024-07-10T09:00:00.000Z',
                },
            ],
        )
        db.commit()

    db.close()


def row_to_dict(row):
    return dict(row) if row else None


def _now():
    return datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S.000Z')


# ── Page routes ────────────────────────────────────────────────────────────────

@app.route('/')
def index():
    return redirect(url_for('dashboard'))


@app.route('/dashboard')
def dashboard():
    db       = get_db()
    policies = [row_to_dict(r) for r in db.execute('SELECT * FROM policies ORDER BY created_at').fetchall()]
    claims   = [row_to_dict(r) for r in db.execute('SELECT * FROM claims ORDER BY submitted_at DESC').fetchall()]
    return render_template('dashboard.html', policies=policies, claims=claims)


@app.route('/claims')
def claims_page():
    db            = get_db()
    policy_filter = request.args.get('policy_id', '')
    policies      = [row_to_dict(r) for r in db.execute('SELECT * FROM policies ORDER BY created_at').fetchall()]

    if policy_filter:
        claims = [row_to_dict(r) for r in db.execute(
            'SELECT * FROM claims WHERE policy_id = ? ORDER BY submitted_at DESC',
            (policy_filter,),
        ).fetchall()]
    else:
        claims = [row_to_dict(r) for r in db.execute(
            'SELECT * FROM claims ORDER BY submitted_at DESC',
        ).fetchall()]

    return render_template('claims.html', claims=claims, policies=policies,
                           selected_policy=policy_filter)


# ── API routes ─────────────────────────────────────────────────────────────────

@app.route('/api/health')
def api_health():
    return jsonify({'status': 'ok', 'timestamp': _now()})


@app.route('/api/policies')
def api_get_policies():
    rows = get_db().execute('SELECT * FROM policies ORDER BY created_at').fetchall()
    return jsonify([row_to_dict(r) for r in rows])


@app.route('/api/policies/<pid>')
def api_get_policy(pid):
    row = get_db().execute('SELECT * FROM policies WHERE id = ?', (pid,)).fetchone()
    if not row:
        return jsonify({'error': 'Policy not found'}), 404
    return jsonify(row_to_dict(row))


@app.route('/api/claims', methods=['GET'])
def api_get_claims():
    pid = request.args.get('policy_id')
    if pid:
        rows = get_db().execute(
            'SELECT * FROM claims WHERE policy_id = ? ORDER BY submitted_at DESC', (pid,),
        ).fetchall()
    else:
        rows = get_db().execute('SELECT * FROM claims ORDER BY submitted_at DESC').fetchall()
    return jsonify([row_to_dict(r) for r in rows])


@app.route('/api/claims', methods=['POST'])
def api_create_claim():
    policy_id   = (request.form.get('policy_id') or '').strip()
    amount_str  = (request.form.get('amount') or '').strip()
    description = (request.form.get('description') or '').strip()

    if not policy_id or not amount_str or not description:
        return jsonify({'error': 'policy_id, amount, and description are required'}), 400

    db = get_db()
    if not db.execute('SELECT 1 FROM policies WHERE id = ?', (policy_id,)).fetchone():
        return jsonify({'error': 'Policy not found'}), 404

    try:
        amount = float(amount_str)
        if amount <= 0:
            raise ValueError
    except ValueError:
        return jsonify({'error': 'Amount must be a positive number'}), 400

    file_name = None
    stored_file_name = None
    f = request.files.get('file')
    if f and f.filename:
        ext = os.path.splitext(f.filename)[1].lower().lstrip('.')
        if ext not in ALLOWED:
            return jsonify({'error': 'Only PDF, JPG, and PNG files are allowed'}), 400
        safe = secure_filename(f.filename)
        stored_file_name = f'{int(time.time() * 1000)}_{safe}'
        f.save(os.path.join(UPLOAD_DIR, stored_file_name))
        file_name = f.filename

    claim_id = f'CLM-{int(time.time() * 1000)}'
    now      = _now()

    db.execute(
        '''INSERT INTO claims (id, policy_id, amount, description, status, file_name, stored_file_name, submitted_at, updated_at)
           VALUES (?, ?, ?, ?, 'Pending', ?, ?, ?, ?)''',
        (claim_id, policy_id, amount, description, file_name, stored_file_name, now, now),
    )
    db.commit()

    claim = row_to_dict(db.execute('SELECT * FROM claims WHERE id = ?', (claim_id,)).fetchone())
    return jsonify(claim), 201


@app.route('/api/claims/<cid>/status', methods=['PATCH'])
def api_update_claim_status(cid):
    data   = request.get_json(silent=True) or {}
    status = data.get('status')
    valid  = ['Pending', 'Approved', 'Rejected']
    if status not in valid:
        return jsonify({'error': f'Status must be one of: {", ".join(valid)}'}), 400

    db = get_db()
    if not db.execute('SELECT 1 FROM claims WHERE id = ?', (cid,)).fetchone():
        return jsonify({'error': 'Claim not found'}), 404

    db.execute('UPDATE claims SET status = ?, updated_at = ? WHERE id = ?', (status, _now(), cid))
    db.commit()

    claim = row_to_dict(db.execute('SELECT * FROM claims WHERE id = ?', (cid,)).fetchone())
    return jsonify(claim)


@app.route('/api/claims/<cid>', methods=['DELETE'])
def api_delete_claim(cid):
    db = get_db()
    if not db.execute('SELECT 1 FROM claims WHERE id = ?', (cid,)).fetchone():
        return jsonify({'error': 'Claim not found'}), 404
    db.execute('DELETE FROM claims WHERE id = ?', (cid,))
    db.commit()
    return '', 204


@app.route('/api/claims/<cid>/document')
def api_download_claim_document(cid):
    row = get_db().execute(
        'SELECT file_name, stored_file_name FROM claims WHERE id = ?',
        (cid,),
    ).fetchone()
    if not row or not row['file_name'] or not row['stored_file_name']:
        return jsonify({'error': 'Claim document not found'}), 404

    file_path = os.path.join(UPLOAD_DIR, row['stored_file_name'])
    if not os.path.exists(file_path):
        return jsonify({'error': 'Claim document not found'}), 404

    return send_from_directory(
        UPLOAD_DIR,
        row['stored_file_name'],
        as_attachment=True,
        download_name=row['file_name'],
    )


@app.route('/api/policies', methods=['POST'])
def api_create_policy():
    data            = request.get_json(silent=True) or {}
    holder_name     = str(data.get('holder_name') or '').strip()
    plan_name       = str(data.get('plan_name') or '').strip()
    coverage_str    = str(data.get('coverage_amount') or '').strip()
    status          = str(data.get('status') or 'active').strip()
    start_date      = str(data.get('start_date') or '').strip()
    end_date        = str(data.get('end_date') or '').strip()

    if not all([holder_name, plan_name, coverage_str, start_date, end_date]):
        return jsonify({'error': 'holder_name, plan_name, coverage_amount, start_date, and end_date are required'}), 400

    if status not in ('active', 'inactive'):
        return jsonify({'error': 'status must be active or inactive'}), 400

    try:
        coverage_amount = float(coverage_str)
        if coverage_amount <= 0:
            raise ValueError
    except ValueError:
        return jsonify({'error': 'coverage_amount must be a positive number'}), 400

    db   = get_db()
    year = datetime.now(timezone.utc).year
    count = db.execute(
        'SELECT COUNT(*) FROM policies WHERE id LIKE ?', (f'POL-{year}-%',)
    ).fetchone()[0]

    policy_id = f'POL-{year}-{count + 1:03d}'
    while db.execute('SELECT 1 FROM policies WHERE id = ?', (policy_id,)).fetchone():
        count += 1
        policy_id = f'POL-{year}-{count:03d}'

    now = _now()
    db.execute(
        '''INSERT INTO policies (id, holder_name, plan_name, coverage_amount, status, start_date, end_date, created_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?)''',
        (policy_id, holder_name, plan_name, coverage_amount, status, start_date, end_date, now),
    )
    db.commit()
    return jsonify(row_to_dict(db.execute('SELECT * FROM policies WHERE id = ?', (policy_id,)).fetchone())), 201


@app.route('/api/policies/<pid>', methods=['PATCH'])
def api_update_policy(pid):
    db = get_db()
    if not db.execute('SELECT 1 FROM policies WHERE id = ?', (pid,)).fetchone():
        return jsonify({'error': 'Policy not found'}), 404

    data   = request.get_json(silent=True) or {}
    fields = {}

    if 'holder_name' in data:
        fields['holder_name'] = str(data['holder_name']).strip()
    if 'plan_name' in data:
        fields['plan_name'] = str(data['plan_name']).strip()
    if 'coverage_amount' in data:
        try:
            val = float(data['coverage_amount'])
            if val <= 0:
                raise ValueError
            fields['coverage_amount'] = val
        except ValueError:
            return jsonify({'error': 'coverage_amount must be a positive number'}), 400
    if 'status' in data:
        if data['status'] not in ('active', 'inactive'):
            return jsonify({'error': 'status must be active or inactive'}), 400
        fields['status'] = data['status']
    if 'start_date' in data:
        fields['start_date'] = str(data['start_date']).strip()
    if 'end_date' in data:
        fields['end_date'] = str(data['end_date']).strip()

    if not fields:
        return jsonify({'error': 'No updatable fields provided'}), 400

    set_clause = ', '.join(f'{k} = ?' for k in fields)
    db.execute(f'UPDATE policies SET {set_clause} WHERE id = ?', (*fields.values(), pid))
    db.commit()
    return jsonify(row_to_dict(db.execute('SELECT * FROM policies WHERE id = ?', (pid,)).fetchone()))


@app.route('/api/policies/<pid>', methods=['DELETE'])
def api_delete_policy(pid):
    db = get_db()
    if not db.execute('SELECT 1 FROM policies WHERE id = ?', (pid,)).fetchone():
        return jsonify({'error': 'Policy not found'}), 404
    db.execute('DELETE FROM claims WHERE policy_id = ?', (pid,))
    db.execute('DELETE FROM policies WHERE id = ?', (pid,))
    db.commit()
    return '', 204


# ── Bootstrap ──────────────────────────────────────────────────────────────────

if __name__ == '__main__':
    init_db()
    print('InsureWell running on http://localhost:5001')
    app.run(host='0.0.0.0', port=5001, debug=True)
