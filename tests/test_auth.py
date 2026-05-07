import os
import tempfile
import unittest

import app as insure_app


class AuthTests(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        insure_app.DB_PATH = os.path.join(self.temp_dir.name, 'test.db')
        insure_app.UPLOAD_DIR = os.path.join(self.temp_dir.name, 'uploads')
        os.makedirs(insure_app.UPLOAD_DIR, exist_ok=True)
        insure_app.app.config.update(
            TESTING=True,
            SECRET_KEY='test-secret',
            SESSION_COOKIE_SECURE=False,
            SESSION_IDLE_TIMEOUT_MINUTES=30,
        )
        insure_app.init_db()
        self.client = insure_app.app.test_client()

    def tearDown(self):
        self.temp_dir.cleanup()

    def login(self, username='alex', password='InsureWell@123'):
        return self.client.post('/login', data={'username': username, 'password': password}, follow_redirects=False)

    def test_redirects_to_login_when_unauthenticated(self):
        resp = self.client.get('/dashboard', follow_redirects=False)
        self.assertEqual(resp.status_code, 302)
        self.assertIn('/login', resp.headers['Location'])

    def test_api_requires_authentication(self):
        resp = self.client.get('/api/policies')
        self.assertEqual(resp.status_code, 401)

    def test_successful_login_sets_session(self):
        resp = self.login()
        self.assertEqual(resp.status_code, 302)
        self.assertIn('/dashboard', resp.headers['Location'])
        dashboard = self.client.get('/dashboard')
        self.assertEqual(dashboard.status_code, 200)
        self.assertIn(b'Hi, alex', dashboard.data)

    def test_invalid_credentials_show_generic_error(self):
        resp = self.client.post('/login', data={'username': 'alex', 'password': 'wrong-password'})
        self.assertEqual(resp.status_code, 200)
        self.assertIn(b'Invalid username or password.', resp.data)

    def test_account_locks_after_five_failed_attempts(self):
        for _ in range(5):
            self.client.post('/login', data={'username': 'alex', 'password': 'wrong-password'})
        locked = self.login(username='alex', password='InsureWell@123')
        self.assertEqual(locked.status_code, 429)
        self.assertIn(b'temporarily locked', locked.data)

    def test_row_level_policy_access(self):
        self.login(username='alex', password='InsureWell@123')
        policies = self.client.get('/api/policies')
        self.assertEqual(policies.status_code, 200)
        payload = policies.get_json()
        self.assertEqual(len(payload), 1)
        self.assertEqual(payload[0]['id'], 'POL-2024-001')

        other = self.client.get('/api/policies/POL-2024-002')
        self.assertEqual(other.status_code, 404)

    def test_logout_clears_access(self):
        self.login()
        out = self.client.post('/logout', follow_redirects=False)
        self.assertEqual(out.status_code, 302)
        self.assertIn('/login', out.headers['Location'])
        protected = self.client.get('/claims', follow_redirects=False)
        self.assertEqual(protected.status_code, 302)
        self.assertIn('/login', protected.headers['Location'])


if __name__ == '__main__':
    unittest.main()
