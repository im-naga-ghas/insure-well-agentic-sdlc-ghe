import json
import os
import secrets
import sqlite3
import time
from datetime import datetime, timedelta, timezone
from functools import wraps

from flask import (Flask, g, jsonify, redirect, render_template,
                   request, session, url_for)
from werkzeug.security import check_password_hash, generate_password_hash
from werkzeug.utils import secure_filename

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 5 * 1024 * 1024  # 5 MB
_secret_key = os.environ.get('SECRET_KEY')
if not _secret_key and os.environ.get('FLASK_ENV') == 'production':
    raise RuntimeError('SECRET_KEY must be set in production')
app.config['SECRET_KEY'] = _secret_key or secrets.token_hex(32)
app.config['SESSION_COOKIE_HTTPONLY'] = True
app.config['SESSION_COOKIE_SECURE'] = os.environ.get('SESSION_COOKIE_SECURE', '0') == '1' or os.environ.get('FLASK_ENV') == 'production'
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'
app.config['SESSION_IDLE_TIMEOUT_MINUTES'] = int(os.environ.get('SESSION_IDLE_TIMEOUT_MINUTES', '30'))
app.config['PERMANENT_SESSION_LIFETIME'] = timedelta(minutes=app.config['SESSION_IDLE_TIMEOUT_MINUTES'])

BASE_DIR   = os.path.dirname(os.path.abspath(__file__))
DATA_DIR   = os.path.join(BASE_DIR, 'data')
DB_PATH    = os.path.join(DATA_DIR, 'insurewell.db')
DATA_FILE  = os.path.join(DATA_DIR, 'store.json')
UPLOAD_DIR = os.path.join(BASE_DIR, 'uploads')
ALLOWED    = {'pdf', 'jpg', 'jpeg', 'png'}
LOGIN_MAX_ATTEMPTS = 5
LOGIN_LOCK_WINDOW_MINUTES = 10

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
        CREATE TABLE IF NOT EXISTS users (
            id                  INTEGER PRIMARY KEY AUTOINCREMENT,
            username            TEXT UNIQUE NOT NULL,
            password_hash       TEXT NOT NULL,
            failed_attempts     INTEGER NOT NULL DEFAULT 0,
            failed_window_start TEXT,
            locked_until        TEXT
        );

        CREATE TABLE IF NOT EXISTS policies (
            id              TEXT PRIMARY KEY,
            owner_user_id   INTEGER REFERENCES users(id),
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
            submitted_at    TEXT NOT NULL,
            updated_at      TEXT NOT NULL
        );
    ''')

    columns = {row[1] for row in db.execute('PRAGMA table_info(policies)').fetchall()}
    if 'owner_user_id' not in columns:
        db.execute('ALTER TABLE policies ADD COLUMN owner_user_id INTEGER REFERENCES users(id)')

    if db.execute('SELECT COUNT(*) FROM users').fetchone()[0] == 0:
        db.executemany(
            '''INSERT INTO users (username, password_hash) VALUES (?, ?)''',
            [
                ('alex', generate_password_hash('InsureWell@123')),
                ('maria', generate_password_hash('InsureWell@123')),
                ('david', generate_password_hash('InsureWell@123')),
            ],
        )

    # Seed only when tables are empty
    if db.execute('SELECT COUNT(*) FROM policies').fetchone()[0] == 0:
        db.executemany(
            '''INSERT INTO policies
               (id, owner_user_id, holder_name, plan_name, coverage_amount, status, start_date, end_date, created_at)
               VALUES (:id, :owner_user_id, :holder_name, :plan_name, :coverage_amount, :status,
                       :start_date, :end_date, :created_at)''',
            [
                {
                    'owner_user_id': 1,
                    'id': 'POL-2024-001', 'holder_name': 'Alex Johnson',
                    'plan_name': 'InsureWell Premium Health Plan',
                    'coverage_amount': 250000, 'status': 'active',
                    'start_date': '2024-01-01', 'end_date': '2026-12-31',
                    'created_at': '2024-01-01T00:00:00.000Z',
                },
                {
                    'owner_user_id': 2,
                    'id': 'POL-2024-002', 'holder_name': 'Maria Garcia',
                    'plan_name': 'InsureWell Essential Care Plan',
                    'coverage_amount': 150000, 'status': 'active',
                    'start_date': '2024-03-15', 'end_date': '2026-03-14',
                    'created_at': '2024-03-15T00:00:00.000Z',
                },
                {
                    'owner_user_id': 3,
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
               (id, policy_id, amount, description, status, file_name, submitted_at, updated_at)
               VALUES (:id, :policy_id, :amount, :description, :status,
                       :file_name, :submitted_at, :updated_at)''',
            [
                {
                    'id': 'CLM-1001', 'policy_id': 'POL-2024-001', 'amount': 3200,
                    'description': 'Emergency room visit – chest pain evaluation',
                    'status': 'Approved', 'file_name': 'er_receipt_jan.pdf',
                    'submitted_at': '2024-02-10T09:15:00.000Z',
                    'updated_at':   '2024-02-14T11:00:00.000Z',
                },
                {
                    'id': 'CLM-1002', 'policy_id': 'POL-2024-001', 'amount': 850,
                    'description': 'Specialist consultation – orthopedics follow-up',
                    'status': 'Approved', 'file_name': 'ortho_invoice.pdf',
                    'submitted_at': '2024-03-05T14:30:00.000Z',
                    'updated_at':   '2024-03-09T10:20:00.000Z',
                },
                {
                    'id': 'CLM-1003', 'policy_id': 'POL-2024-001', 'amount': 12500,
                    'description': 'Knee surgery – arthroscopic procedure',
                    'status': 'Pending', 'file_name': 'surgery_discharge_summary.pdf',
                    'submitted_at': '2024-04-18T08:00:00.000Z',
                    'updated_at':   '2024-04-18T08:00:00.000Z',
                },
                {
                    'id': 'CLM-1004', 'policy_id': 'POL-2024-001', 'amount': 420,
                    'description': 'Prescription medications – 3-month supply',
                    'status': 'Rejected', 'file_name': None,
                    'submitted_at': '2024-05-02T16:45:00.000Z',
                    'updated_at':   '2024-05-07T09:30:00.000Z',
                },
                {
                    'id': 'CLM-1005', 'policy_id': 'POL-2024-001', 'amount': 1750,
                    'description': 'MRI scan – lower back pain diagnosis',
                    'status': 'Pending', 'file_name': 'mri_report.jpg',
                    'submitted_at': '2024-06-20T11:00:00.000Z',
                    'updated_at':   '2024-06-20T11:00:00.000Z',
                },
                {
                    'id': 'CLM-2001', 'policy_id': 'POL-2024-002', 'amount': 600,
                    'description': 'Annual physical exam and blood work panel',
                    'status': 'Approved', 'file_name': 'lab_results.pdf',
                    'submitted_at': '2024-04-01T10:00:00.000Z',
                    'updated_at':   '2024-04-05T14:00:00.000Z',
                },
                {
                    'id': 'CLM-2002', 'policy_id': 'POL-2024-002', 'amount': 2300,
                    'description': 'Dental surgery – wisdom tooth extraction',
                    'status': 'Pending', 'file_name': 'dental_xray.png',
                    'submitted_at': '2024-07-10T09:00:00.000Z',
                    'updated_at':   '2024-07-10T09:00:00.000Z',
                },
            ],
        )
        db.commit()

    db.execute('''
        UPDATE policies
        SET owner_user_id = (SELECT id FROM users WHERE username = 'alex')
        WHERE holder_name = 'Alex Johnson' AND owner_user_id IS NULL
    ''')
    db.execute('''
        UPDATE policies
        SET owner_user_id = (SELECT id FROM users WHERE username = 'maria')
        WHERE holder_name = 'Maria Garcia' AND owner_user_id IS NULL
    ''')
    db.execute('''
        UPDATE policies
        SET owner_user_id = (SELECT id FROM users WHERE username = 'david')
        WHERE holder_name = 'David Chen' AND owner_user_id IS NULL
    ''')
    db.commit()

    db.close()


def row_to_dict(row):
    return dict(row) if row else None


def _now():
    return datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S.000Z')


def _parse_ts(ts):
    if not ts:
        return None
    try:
        return datetime.fromisoformat(ts.replace('Z', '+00:00'))
    except ValueError:
        return None


def _current_user():
    if hasattr(g, 'current_user'):
        return g.current_user
    uid = session.get('user_id')
    if not uid:
        g.current_user = None
    else:
        g.current_user = get_db().execute('SELECT id, username FROM users WHERE id = ?', (uid,)).fetchone()
    return g.current_user


def _auth_error_response():
    if request.path.startswith('/api/'):
        return jsonify({'error': 'Authentication required'}), 401
    return redirect(url_for('login'))


def login_required(fn):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        if not _current_user():
            return _auth_error_response()
        return fn(*args, **kwargs)
    return wrapper


@app.before_request
def enforce_authentication():
    if request.endpoint in (None, 'login', 'logout', 'static'):
        return None

    user = _current_user()
    if not user:
        return _auth_error_response()

    now = datetime.now(timezone.utc)
    last_activity = _parse_ts(session.get('last_activity'))
    timeout_minutes = app.config.get('SESSION_IDLE_TIMEOUT_MINUTES', 30)
    if last_activity and (now - last_activity) > timedelta(minutes=timeout_minutes):
        session.clear()
        return _auth_error_response()

    session.permanent = True
    session['last_activity'] = _now()
    return None


@app.route('/login', methods=['GET', 'POST'])
def login():
    if _current_user():
        return redirect(url_for('dashboard'))

    error = None
    if request.method == 'POST':
        username = (request.form.get('username') or '').strip()
        password = request.form.get('password') or ''
        now = datetime.now(timezone.utc)

        user = get_db().execute('SELECT * FROM users WHERE username = ?', (username,)).fetchone()
        if user:
            locked_until = _parse_ts(user['locked_until'])
            if locked_until and locked_until > now:
                error = 'Your account is temporarily locked. Please try again later.'
                return render_template('login.html', error=error), 429

            if locked_until and locked_until <= now:
                get_db().execute(
                    'UPDATE users SET failed_attempts = 0, failed_window_start = NULL, locked_until = NULL WHERE id = ?',
                    (user['id'],),
                )
                get_db().commit()

        if user and check_password_hash(user['password_hash'], password):
            session.clear()
            session.permanent = True
            session['user_id'] = user['id']
            session['last_activity'] = _now()
            get_db().execute(
                'UPDATE users SET failed_attempts = 0, failed_window_start = NULL, locked_until = NULL WHERE id = ?',
                (user['id'],),
            )
            get_db().commit()
            return redirect(url_for('dashboard'))

        if user:
            window_start = _parse_ts(user['failed_window_start'])
            attempts = user['failed_attempts'] or 0
            if not window_start or (now - window_start) > timedelta(minutes=LOGIN_LOCK_WINDOW_MINUTES):
                attempts = 1
                window_start = now
            else:
                attempts += 1

            locked_until = None
            if attempts >= LOGIN_MAX_ATTEMPTS:
                locked_until = now + timedelta(minutes=LOGIN_LOCK_WINDOW_MINUTES)
                error = 'Your account is temporarily locked. Please try again later.'
            else:
                error = 'Invalid username or password.'

            get_db().execute(
                'UPDATE users SET failed_attempts = ?, failed_window_start = ?, locked_until = ? WHERE id = ?',
                (attempts, window_start.strftime('%Y-%m-%dT%H:%M:%S.000Z'),
                 locked_until.strftime('%Y-%m-%dT%H:%M:%S.000Z') if locked_until else None, user['id']),
            )
            get_db().commit()
        else:
            error = 'Invalid username or password.'

    return render_template('login.html', error=error)


@app.route('/logout', methods=['POST'])
def logout():
    session.clear()
    return redirect(url_for('login'))


# ── Page routes ────────────────────────────────────────────────────────────────

@app.route('/')
def index():
    if _current_user():
        return redirect(url_for('dashboard'))
    return redirect(url_for('login'))


@app.route('/dashboard')
@login_required
def dashboard():
    user_id = _current_user()['id']
    db       = get_db()
    policies = [row_to_dict(r) for r in db.execute(
        'SELECT * FROM policies WHERE owner_user_id = ? ORDER BY created_at',
        (user_id,),
    ).fetchall()]
    claims   = [row_to_dict(r) for r in db.execute(
        '''SELECT c.* FROM claims c
           JOIN policies p ON p.id = c.policy_id
           WHERE p.owner_user_id = ?
           ORDER BY c.submitted_at DESC''',
        (user_id,),
    ).fetchall()]
    return render_template('dashboard.html', policies=policies, claims=claims)


@app.route('/claims')
@login_required
def claims_page():
    user_id       = _current_user()['id']
    db            = get_db()
    policy_filter = request.args.get('policy_id', '')
    policies      = [row_to_dict(r) for r in db.execute(
        'SELECT * FROM policies WHERE owner_user_id = ? ORDER BY created_at',
        (user_id,),
    ).fetchall()]

    if policy_filter:
        if not db.execute('SELECT 1 FROM policies WHERE id = ? AND owner_user_id = ?', (policy_filter, user_id)).fetchone():
            return render_template('claims.html', claims=[], policies=policies, selected_policy='')
        claims = [row_to_dict(r) for r in db.execute(
            '''SELECT c.* FROM claims c
               JOIN policies p ON p.id = c.policy_id
               WHERE c.policy_id = ? AND p.owner_user_id = ?
               ORDER BY c.submitted_at DESC''',
            (policy_filter, user_id),
        ).fetchall()]
    else:
        claims = [row_to_dict(r) for r in db.execute(
            '''SELECT c.* FROM claims c
               JOIN policies p ON p.id = c.policy_id
               WHERE p.owner_user_id = ?
               ORDER BY c.submitted_at DESC''',
            (user_id,),
        ).fetchall()]

    return render_template('claims.html', claims=claims, policies=policies,
                           selected_policy=policy_filter)


# ── API routes ─────────────────────────────────────────────────────────────────

@app.route('/api/health')
@login_required
def api_health():
    return jsonify({'status': 'ok', 'timestamp': _now()})


@app.route('/api/policies')
@login_required
def api_get_policies():
    rows = get_db().execute(
        'SELECT * FROM policies WHERE owner_user_id = ? ORDER BY created_at',
        (_current_user()['id'],),
    ).fetchall()
    return jsonify([row_to_dict(r) for r in rows])


@app.route('/api/policies/<pid>')
@login_required
def api_get_policy(pid):
    row = get_db().execute(
        'SELECT * FROM policies WHERE id = ? AND owner_user_id = ?',
        (pid, _current_user()['id']),
    ).fetchone()
    if not row:
        return jsonify({'error': 'Policy not found'}), 404
    return jsonify(row_to_dict(row))


@app.route('/api/claims', methods=['GET'])
@login_required
def api_get_claims():
    user_id = _current_user()['id']
    pid = request.args.get('policy_id')
    if pid:
        if not get_db().execute('SELECT 1 FROM policies WHERE id = ? AND owner_user_id = ?', (pid, user_id)).fetchone():
            return jsonify([])
        rows = get_db().execute(
            '''SELECT c.* FROM claims c
               JOIN policies p ON p.id = c.policy_id
               WHERE c.policy_id = ? AND p.owner_user_id = ?
               ORDER BY c.submitted_at DESC''',
            (pid, user_id),
        ).fetchall()
    else:
        rows = get_db().execute(
            '''SELECT c.* FROM claims c
               JOIN policies p ON p.id = c.policy_id
               WHERE p.owner_user_id = ?
               ORDER BY c.submitted_at DESC''',
            (user_id,),
        ).fetchall()
    return jsonify([row_to_dict(r) for r in rows])


@app.route('/api/claims', methods=['POST'])
@login_required
def api_create_claim():
    user_id     = _current_user()['id']
    policy_id   = (request.form.get('policy_id') or '').strip()
    amount_str  = (request.form.get('amount') or '').strip()
    description = (request.form.get('description') or '').strip()

    if not policy_id or not amount_str or not description:
        return jsonify({'error': 'policy_id, amount, and description are required'}), 400

    db = get_db()
    if not db.execute('SELECT 1 FROM policies WHERE id = ? AND owner_user_id = ?', (policy_id, user_id)).fetchone():
        return jsonify({'error': 'Policy not found'}), 404

    try:
        amount = float(amount_str)
        if amount <= 0:
            raise ValueError
    except ValueError:
        return jsonify({'error': 'Amount must be a positive number'}), 400

    file_name = None
    f = request.files.get('file')
    if f and f.filename:
        ext = os.path.splitext(f.filename)[1].lower().lstrip('.')
        if ext not in ALLOWED:
            return jsonify({'error': 'Only PDF, JPG, and PNG files are allowed'}), 400
        safe = secure_filename(f.filename)
        f.save(os.path.join(UPLOAD_DIR, f'{int(time.time() * 1000)}_{safe}'))
        file_name = f.filename

    claim_id = f'CLM-{int(time.time() * 1000)}'
    now      = _now()

    db.execute(
        '''INSERT INTO claims (id, policy_id, amount, description, status, file_name, submitted_at, updated_at)
           VALUES (?, ?, ?, ?, 'Pending', ?, ?, ?)''',
        (claim_id, policy_id, amount, description, file_name, now, now),
    )
    db.commit()

    claim = row_to_dict(db.execute('SELECT * FROM claims WHERE id = ?', (claim_id,)).fetchone())
    return jsonify(claim), 201


@app.route('/api/claims/<cid>/status', methods=['PATCH'])
@login_required
def api_update_claim_status(cid):
    user_id = _current_user()['id']
    data   = request.get_json(silent=True) or {}
    status = data.get('status')
    valid  = ['Pending', 'Approved', 'Rejected']
    if status not in valid:
        return jsonify({'error': f'Status must be one of: {", ".join(valid)}'}), 400

    db = get_db()
    if not db.execute(
        '''SELECT 1 FROM claims c
           JOIN policies p ON p.id = c.policy_id
           WHERE c.id = ? AND p.owner_user_id = ?''',
        (cid, user_id),
    ).fetchone():
        return jsonify({'error': 'Claim not found'}), 404

    db.execute('UPDATE claims SET status = ?, updated_at = ? WHERE id = ?', (status, _now(), cid))
    db.commit()

    claim = row_to_dict(db.execute('SELECT * FROM claims WHERE id = ?', (cid,)).fetchone())
    return jsonify(claim)


@app.route('/api/claims/<cid>', methods=['DELETE'])
@login_required
def api_delete_claim(cid):
    user_id = _current_user()['id']
    db = get_db()
    if not db.execute(
        '''SELECT 1 FROM claims c
           JOIN policies p ON p.id = c.policy_id
           WHERE c.id = ? AND p.owner_user_id = ?''',
        (cid, user_id),
    ).fetchone():
        return jsonify({'error': 'Claim not found'}), 404
    db.execute('DELETE FROM claims WHERE id = ?', (cid,))
    db.commit()
    return '', 204


@app.route('/api/policies', methods=['POST'])
@login_required
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

    db    = get_db()
    user_id = _current_user()['id']
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
        '''INSERT INTO policies (id, owner_user_id, holder_name, plan_name, coverage_amount, status, start_date, end_date, created_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)''',
        (policy_id, user_id, holder_name, plan_name, coverage_amount, status, start_date, end_date, now),
    )
    db.commit()
    return jsonify(row_to_dict(db.execute('SELECT * FROM policies WHERE id = ?', (policy_id,)).fetchone())), 201


@app.route('/api/policies/<pid>', methods=['PATCH'])
@login_required
def api_update_policy(pid):
    db = get_db()
    if not db.execute('SELECT 1 FROM policies WHERE id = ? AND owner_user_id = ?', (pid, _current_user()['id'])).fetchone():
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
@login_required
def api_delete_policy(pid):
    user_id = _current_user()['id']
    db = get_db()
    if not db.execute('SELECT 1 FROM policies WHERE id = ? AND owner_user_id = ?', (pid, user_id)).fetchone():
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
