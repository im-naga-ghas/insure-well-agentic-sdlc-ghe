import io
import os
import tempfile
import unittest

import app as insurewell_app


class ClaimDocumentDownloadTests(unittest.TestCase):
    def setUp(self):
        self.tmpdir = tempfile.TemporaryDirectory()
        self.data_dir = os.path.join(self.tmpdir.name, 'data')
        self.upload_dir = os.path.join(self.tmpdir.name, 'uploads')

        self._orig_data_dir = insurewell_app.DATA_DIR
        self._orig_db_path = insurewell_app.DB_PATH
        self._orig_data_file = insurewell_app.DATA_FILE
        self._orig_upload_dir = insurewell_app.UPLOAD_DIR

        insurewell_app.DATA_DIR = self.data_dir
        insurewell_app.DB_PATH = os.path.join(self.data_dir, 'insurewell.db')
        insurewell_app.DATA_FILE = os.path.join(self.data_dir, 'store.json')
        insurewell_app.UPLOAD_DIR = self.upload_dir
        insurewell_app.init_db()

        insurewell_app.app.config['TESTING'] = True
        self.client = insurewell_app.app.test_client()

    def tearDown(self):
        insurewell_app.DATA_DIR = self._orig_data_dir
        insurewell_app.DB_PATH = self._orig_db_path
        insurewell_app.DATA_FILE = self._orig_data_file
        insurewell_app.UPLOAD_DIR = self._orig_upload_dir
        self.tmpdir.cleanup()

    def _create_claim(self, with_file=True):
        data = {
            'policy_id': 'POL-2024-001',
            'amount': '100.50',
            'description': 'Test claim',
        }
        if with_file:
            data['file'] = (io.BytesIO(b'claim-document-body'), 'receipt.pdf')
        resp = self.client.post('/api/claims', data=data, content_type='multipart/form-data')
        self.assertEqual(resp.status_code, 201)
        return resp.get_json()['id']

    def test_download_claim_document_success(self):
        claim_id = self._create_claim(with_file=True)

        resp = self.client.get(f'/api/claims/{claim_id}/document')
        try:
            self.assertEqual(resp.status_code, 200)
            self.assertIn('attachment;', resp.headers.get('Content-Disposition', ''))
            self.assertIn('receipt.pdf', resp.headers.get('Content-Disposition', ''))
            self.assertEqual(resp.data, b'claim-document-body')
        finally:
            resp.close()

    def test_download_claim_document_not_found_without_file(self):
        claim_id = self._create_claim(with_file=False)

        resp = self.client.get(f'/api/claims/{claim_id}/document')
        self.assertEqual(resp.status_code, 404)
        self.assertEqual(resp.get_json()['error'], 'Claim document not found')

    def test_download_claim_document_not_found_when_file_missing(self):
        claim_id = self._create_claim(with_file=True)
        with insurewell_app.app.app_context():
            row = insurewell_app.get_db().execute(
                'SELECT stored_file_name FROM claims WHERE id = ?',
                (claim_id,),
            ).fetchone()

        os.remove(os.path.join(insurewell_app.UPLOAD_DIR, row['stored_file_name']))
        resp = self.client.get(f'/api/claims/{claim_id}/document')
        self.assertEqual(resp.status_code, 404)
        self.assertEqual(resp.get_json()['error'], 'Claim document not found')


if __name__ == '__main__':
    unittest.main()
