import os
import tempfile
import unittest

import app as insurewell


class ClaimDocumentDownloadTests(unittest.TestCase):
    def setUp(self):
        self.tempdir = tempfile.TemporaryDirectory()
        self.addCleanup(self.tempdir.cleanup)

        insurewell.DATA_DIR = os.path.join(self.tempdir.name, 'data')
        insurewell.DB_PATH = os.path.join(insurewell.DATA_DIR, 'insurewell.db')
        insurewell.UPLOAD_DIR = os.path.join(self.tempdir.name, 'uploads')
        insurewell.app.config['TESTING'] = True

        insurewell.init_db()
        self.client = insurewell.app.test_client()

        with insurewell.app.app_context():
            db = insurewell.get_db()
            db.execute('DELETE FROM claims')
            db.execute('DELETE FROM policies')
            db.executemany(
                '''INSERT INTO policies
                   (id, holder_name, plan_name, coverage_amount, status, start_date, end_date, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)''',
                [
                    ('POL-ALPHA', 'Alice Policyholder', 'Premium', 100000, 'active', '2024-01-01', '2025-01-01', '2024-01-01T00:00:00.000Z'),
                    ('POL-BETA', 'Bob Policyholder', 'Essential', 50000, 'active', '2024-01-01', '2025-01-01', '2024-01-01T00:00:00.000Z'),
                ],
            )
            db.commit()

    def _insert_claim(self, claim_id, policy_id, file_name):
        with insurewell.app.app_context():
            db = insurewell.get_db()
            db.execute(
                '''INSERT INTO claims (id, policy_id, amount, description, status, file_name, submitted_at, updated_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)''',
                (
                    claim_id,
                    policy_id,
                    123.45,
                    'Test claim',
                    'Pending',
                    file_name,
                    '2024-01-01T00:00:00.000Z',
                    '2024-01-01T00:00:00.000Z',
                ),
            )
            db.commit()

    def test_downloads_attached_document_as_attachment(self):
        file_path = os.path.join(insurewell.UPLOAD_DIR, 'receipt.pdf')
        with open(file_path, 'wb') as fh:
            fh.write(b'%PDF-1.4 sample')

        self._insert_claim('CLM-OK', 'POL-ALPHA', 'receipt.pdf')

        response = self.client.get('/api/claims/CLM-OK/document', headers={'X-Policy-ID': 'POL-ALPHA'})
        try:
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.mimetype, 'application/pdf')
            self.assertEqual(response.data, b'%PDF-1.4 sample')
            self.assertIn('attachment;', response.headers['Content-Disposition'])
            self.assertIn('receipt.pdf', response.headers['Content-Disposition'])
        finally:
            response.close()

    def test_rejects_cross_policy_document_access(self):
        file_path = os.path.join(insurewell.UPLOAD_DIR, 'receipt.pdf')
        with open(file_path, 'wb') as fh:
            fh.write(b'%PDF-1.4 sample')

        self._insert_claim('CLM-FORBIDDEN', 'POL-ALPHA', 'receipt.pdf')

        response = self.client.get('/api/claims/CLM-FORBIDDEN/document', headers={'X-Policy-ID': 'POL-BETA'})

        self.assertEqual(response.status_code, 403)
        self.assertEqual(response.get_json()['error'], 'You are not allowed to access this claim document')

    def test_returns_404_when_claim_has_no_document(self):
        self._insert_claim('CLM-NODOC', 'POL-ALPHA', None)

        response = self.client.get('/api/claims/CLM-NODOC/document', headers={'X-Policy-ID': 'POL-ALPHA'})

        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.get_json()['error'], 'No document is attached to this claim')

    def test_rejects_path_traversal_file_names(self):
        safe_copy_path = os.path.join(insurewell.UPLOAD_DIR, 'secret.txt')
        with open(safe_copy_path, 'wb') as fh:
            fh.write(b'should-not-be-served')

        self._insert_claim('CLM-TRAVERSAL', 'POL-ALPHA', '../secret.txt')

        response = self.client.get('/api/claims/CLM-TRAVERSAL/document', headers={'X-Policy-ID': 'POL-ALPHA'})

        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.get_json()['error'], 'Claim document could not be found')


if __name__ == '__main__':
    unittest.main()
