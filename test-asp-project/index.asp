<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>ASP Test Page</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            padding: 20px;
            background: #f0f0f0;
        }
        .test-section {
            background: white;
            padding: 20px;
            margin: 10px 0;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 { color: #333; }
        .pass { color: green; }
        .fail { color: red; }
    </style>
</head>
<body>
    <h1>ASP Plugin Test Page</h1>
    
    <div class="test-section">
        <h2>Test 1: HTML Syntax Highlighting</h2>
        <p>If you see proper colors for tags, attributes, and text - <span class="pass">PASS</span></p>
        <div id="test-div" class="test-class">
            <span>Nested element</span>
        </div>
    </div>
    
    <div class="test-section">
        <h2>Test 2: Auto-completion</h2>
        <p>Type &lt;di and press Ctrl+Space</p>
        <!-- Try here: -->
        
    </div>
    
    <div class="test-section">
        <h2>Test 3: Tag Matching</h2>
        <p>Click on opening tag to highlight closing tag:</p>
        <div>
            <p>Paragraph 1</p>
            <p>Paragraph 2</p>
        </div>
    </div>
    
    <div class="test-section">
        <h2>Test 4: Validation</h2>
        <p>This should show an error (unclosed tag):</p>
        <!-- <div>This div is not closed -->
    </div>
    
    <footer>
        <p><small>ASP Classic Language Support - Test Suite</small></p>
    </footer>
</body>
</html>

