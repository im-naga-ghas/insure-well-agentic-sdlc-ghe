# Copilot Instructions for Memory Game Application

## Project Overview
Full-stack memory matching game built entirely with Python Flask serving HTML/CSS/JavaScript frontend.

**Tech Stack:**
- Backend: Python 3.9+, Flask 3.0
- Frontend: HTML5, CSS3, Vanilla JavaScript (served by Flask)
- Deployment: Render.com, Railway, or GitHub Pages (static export)
- Testing: pytest

---

## 1. Project Structure

```
backend/
├── app.py              # Flask application
├── requirements.txt    # Python dependencies
├── test_app.py         # Pytest tests
├── render.yaml         # Render deployment config
├── templates/
│   └── index.html      # Game HTML (Jinja2 template)
└── static/
    ├── css/
    │   └── styles.css  # Game styles
    └── js/
        └── game.js     # Game logic
```

---

## 2. Backend Development (Flask)

### API Design
- Follow RESTful conventions
- Return JSON with proper status codes
- Serve frontend via Flask templates

### Endpoints
```python
GET    /                    # Main game page (HTML)
GET    /api                 # API information
GET    /api/leaderboard     # Top scores (?limit)
POST   /api/score           # Submit new score
GET    /api/stats           # Game statistics
GET    /api/health          # Health check
```

### Error Handling & Validation
```python
@app.route('/api/score', methods=['POST'])
def create_score():
    data = request.get_json()
    
    # Validate required fields
    if not all(k in data for k in ['playerName', 'score']):
        return jsonify({'error': 'Missing fields'}), 400
    
    # Validate data types/ranges
    if not isinstance(data['score'], int) or data['score'] < 0:
        return jsonify({'error': 'Invalid score'}), 400
    
    # Save score
    leaderboard.append({
        'id': str(uuid.uuid4()),
        'playerName': data['playerName'],
        'score': data['score'],
        'moves': data.get('moves', 0),
        'time': data.get('time', 0),
        'timestamp': datetime.now().isoformat()
    })
    
    return jsonify({'success': True}), 201
```

### Best Practices
- Use type hints: `def func(name: str) -> dict:`
- Add docstrings to functions
- Follow PEP 8 style guide
- Use environment variables for config

### Commands
```bash
python app.py                    # Run server (localhost:5000)
python -m venv venv             # Create virtual environment
pip install -r requirements.txt
pip freeze > requirements.txt
gunicorn app:app                # Production server
```

---

## 3. Frontend Development (JavaScript)

### Structure
```javascript
// Game state management
const gameState = {
    cards: [],
    flippedCards: [],
    matchedPairs: 0,
    moves: 0,
    timer: null,
    timeElapsed: 0,
    isLocked: false
};

// Core functions
function initGame() { }
function flipCard(card) { }
function checkMatch() { }
function calculateScore() { }
function submitScore(playerName) { }
```

### Best Practices
- Use `const` and `let` (no `var`)
- Keep functions small and focused
- Use event delegation where appropriate
- Handle API errors gracefully

### Styling (CSS)
```css
:root {
    --primary: #667eea;
    --secondary: #764ba2;
    --card-size: 100px;
}

.game-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
}
```

---

## 4. Unit Testing (pytest)

### Test Examples
```python
def test_api_info(client):
    """Test API info endpoint."""
    response = client.get('/api')
    assert response.status_code == 200
    assert 'endpoints' in response.json

def test_create_score(client):
    """Test score submission."""
    response = client.post('/api/score', json={
        'playerName': 'Test',
        'score': 500,
        'moves': 20,
        'time': 45
    })
    assert response.status_code == 201

def test_invalid_score(client):
    """Test invalid score rejection."""
    response = client.post('/api/score', json={
        'playerName': 'Test',
        'score': -100  # Invalid
    })
    assert response.status_code == 400

def test_leaderboard(client):
    """Test leaderboard retrieval."""
    response = client.get('/api/leaderboard')
    assert response.status_code == 200
    assert isinstance(response.json, list)

def test_health_check(client):
    """Test health endpoint."""
    response = client.get('/api/health')
    assert response.status_code == 200
    assert response.json['status'] == 'healthy'
```

### Coverage
- All API endpoints
- Validation logic
- Error handling
- Edge cases

### Commands
```bash
pytest                          # Run all tests
pytest -v                       # Verbose output
pytest --cov=. --cov-report=term  # With coverage
pytest --cov=. --cov-report=html  # HTML coverage report
```

---

## 5. End-to-End Testing

### Tools
- **Playwright** or **Selenium** for E2E testing

### Example E2E Test (Playwright)
```python
from playwright.sync_api import sync_playwright

def test_complete_game():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        page.goto('http://localhost:5000')
        
        # Verify game loads
        assert page.locator('.game-grid').is_visible()
        
        # Click cards
        cards = page.locator('.card')
        cards.nth(0).click()
        cards.nth(1).click()
        
        # Submit score after game
        page.fill('#playerName', 'E2E Test')
        page.click('button:has-text("Submit")')
        
        browser.close()
```

---

## 6. GitHub Actions Workflows

### CI/CD Pipeline
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        python-version: ['3.9', '3.10', '3.11', '3.12']
    
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: ${{ matrix.python-version }}
      
      - name: Install dependencies
        run: |
          cd backend
          pip install -r requirements.txt
      
      - name: Lint with flake8
        run: |
          cd backend
          flake8 . --max-line-length=127
      
      - name: Test with pytest
        run: |
          cd backend
          pytest -v --cov=. --cov-report=term
```

---

## 7. Documentation

### README Structure
```markdown
# Memory Game

## Features
- 3x3 grid memory card game
- Real-time timer and scoring
- Leaderboard API

## Quick Start
cd backend
python -m venv venv
source venv/bin/activate  # or venv\Scripts\activate on Windows
pip install -r requirements.txt
python app.py

## API Endpoints
- GET /api/leaderboard
- POST /api/score

## Testing
pytest -v --cov=.

## Deployment
gunicorn app:app --bind 0.0.0.0:5000
```

---

## 8. Development Guidelines

### Git Workflow
```bash
# Feature branches
git checkout -b feature/add-themes
git checkout -b fix/score-calculation

# Commit messages
git commit -m "feat: add dark theme support"
git commit -m "fix: correct score calculation"
git commit -m "docs: update API documentation"
```

### Code Quality
- Follow PEP 8 for Python
- Use ESLint for JavaScript (optional)
- Keep functions under 50 lines
- Write descriptive variable names

### Security
- Validate all user inputs
- Sanitize data before storage
- Use environment variables for secrets
- Enable HTTPS in production

### Performance
- Add database indexes for leaderboard queries
- Cache frequently accessed data
- Optimize static assets
- Use gzip compression

---

## 9. Error Handling

### Backend (Flask) Error Handling

#### Global Error Handlers
```python
@app.errorhandler(400)
def bad_request(error):
    """Handle 400 Bad Request errors."""
    return jsonify({
        'error': 'Bad Request',
        'message': str(error.description) if hasattr(error, 'description') else 'Invalid request'
    }), 400

@app.errorhandler(404)
def not_found(error):
    """Handle 404 Not Found errors."""
    return jsonify({
        'error': 'Not Found',
        'message': 'The requested resource was not found'
    }), 404

@app.errorhandler(500)
def internal_error(error):
    """Handle 500 Internal Server errors."""
    app.logger.error(f'Server Error: {error}')
    return jsonify({
        'error': 'Internal Server Error',
        'message': 'An unexpected error occurred'
    }), 500

@app.errorhandler(Exception)
def handle_exception(error):
    """Handle all unhandled exceptions."""
    app.logger.error(f'Unhandled Exception: {error}', exc_info=True)
    return jsonify({
        'error': 'Server Error',
        'message': 'An unexpected error occurred'
    }), 500
```

#### Route-Level Error Handling
```python
@app.route('/api/score', methods=['POST'])
def submit_score():
    try:
        data = request.get_json()
        
        # Validate JSON parsing
        if data is None:
            return jsonify({'error': 'Invalid JSON'}), 400
        
        # Validate required fields
        required = ['playerName', 'score', 'moves', 'time']
        missing = [f for f in required if f not in data]
        if missing:
            return jsonify({
                'error': 'Missing required fields',
                'fields': missing
            }), 400
        
        # Validate data types
        if not isinstance(data['playerName'], str):
            return jsonify({'error': 'playerName must be a string'}), 400
        
        if not isinstance(data['score'], (int, float)) or data['score'] < 0:
            return jsonify({'error': 'score must be a non-negative number'}), 400
        
        # Process and save
        # ...
        
        return jsonify({'success': True}), 201
        
    except json.JSONDecodeError:
        return jsonify({'error': 'Invalid JSON format'}), 400
    except Exception as e:
        app.logger.error(f'Error submitting score: {e}')
        return jsonify({'error': 'Failed to submit score'}), 500
```

#### Custom Exception Classes
```python
class ValidationError(Exception):
    """Raised when input validation fails."""
    def __init__(self, message, field=None):
        self.message = message
        self.field = field
        super().__init__(self.message)

class NotFoundError(Exception):
    """Raised when a resource is not found."""
    pass

@app.errorhandler(ValidationError)
def handle_validation_error(error):
    response = {'error': 'Validation Error', 'message': error.message}
    if error.field:
        response['field'] = error.field
    return jsonify(response), 400
```

### Frontend (JavaScript) Error Handling

#### API Call Error Handling
```javascript
async function submitScore(playerName, score, moves, time) {
    try {
        const response = await fetch('/api/score', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerName, score, moves, time })
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || `HTTP ${response.status}`);
        }
        
        return await response.json();
    } catch (error) {
        if (error.name === 'TypeError') {
            // Network error
            console.error('Network error:', error);
            showError('Unable to connect to server. Please check your connection.');
        } else {
            console.error('API error:', error);
            showError(error.message || 'Failed to submit score');
        }
        throw error;
    }
}

async function fetchLeaderboard() {
    try {
        const response = await fetch('/api/leaderboard');
        if (!response.ok) {
            throw new Error(`Failed to fetch leaderboard: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error('Leaderboard error:', error);
        showError('Failed to load leaderboard');
        return []; // Return empty array as fallback
    }
}
```

#### User-Friendly Error Display
```javascript
function showError(message, duration = 5000) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-toast';
    errorDiv.textContent = message;
    document.body.appendChild(errorDiv);
    
    setTimeout(() => {
        errorDiv.classList.add('fade-out');
        setTimeout(() => errorDiv.remove(), 300);
    }, duration);
}

function showSuccess(message) {
    const successDiv = document.createElement('div');
    successDiv.className = 'success-toast';
    successDiv.textContent = message;
    document.body.appendChild(successDiv);
    
    setTimeout(() => successDiv.remove(), 3000);
}
```

#### Global Error Handling
```javascript
// Catch unhandled promise rejections
window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled promise rejection:', event.reason);
    showError('An unexpected error occurred');
    event.preventDefault();
});

// Catch global errors
window.addEventListener('error', (event) => {
    console.error('Global error:', event.error);
    // Don't show error for script loading failures
    if (event.error) {
        showError('An unexpected error occurred');
    }
});
```

#### Input Validation
```javascript
function validatePlayerName(name) {
    if (!name || typeof name !== 'string') {
        return { valid: false, error: 'Name is required' };
    }
    
    const trimmed = name.trim();
    if (trimmed.length === 0) {
        return { valid: false, error: 'Name cannot be empty' };
    }
    
    if (trimmed.length > 50) {
        return { valid: false, error: 'Name must be 50 characters or less' };
    }
    
    // Only allow alphanumeric and basic punctuation
    if (!/^[a-zA-Z0-9\s\-_]+$/.test(trimmed)) {
        return { valid: false, error: 'Name contains invalid characters' };
    }
    
    return { valid: true, value: trimmed };
}
```

### Logging Best Practices

#### Backend Logging
```python
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Usage in routes
@app.route('/api/score', methods=['POST'])
def submit_score():
    logger.info(f'Score submission request from {request.remote_addr}')
    try:
        # ... process score
        logger.info(f'Score submitted successfully: {score_entry["id"]}')
    except Exception as e:
        logger.error(f'Score submission failed: {e}', exc_info=True)
        raise
```

#### Frontend Logging
```javascript
const Logger = {
    info: (message, data = {}) => {
        console.log(`[INFO] ${message}`, data);
    },
    error: (message, error = null) => {
        console.error(`[ERROR] ${message}`, error);
        // Optional: Send to error tracking service
    },
    debug: (message, data = {}) => {
        if (process.env.NODE_ENV === 'development') {
            console.debug(`[DEBUG] ${message}`, data);
        }
    }
};
```

---

## Quick Reference

**Port:** 5000

**Key Files:**
- [backend/app.py](backend/app.py) - Flask application
- [backend/templates/index.html](backend/templates/index.html) - Game HTML
- [backend/static/js/game.js](backend/static/js/game.js) - Game logic
- [backend/static/css/styles.css](backend/static/css/styles.css) - Styles

**Common Tasks:**
1. Add new API endpoint → Add route in app.py → Add tests → Update docs
2. Modify game logic → Update game.js → Test in browser
3. Change styles → Update styles.css → Verify responsive design
