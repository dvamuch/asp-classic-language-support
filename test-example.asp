<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ASP Classic Test Page</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #333;
        }
        .container {
            max-width: 800px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
        h1 {
            color: #667eea;
            border-bottom: 3px solid #764ba2;
            padding-bottom: 10px;
        }
        .feature-list {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 5px;
            margin: 20px 0;
        }
        .feature-list li {
            margin: 10px 0;
            padding-left: 25px;
            position: relative;
        }
        .feature-list li:before {
            content: "✓";
            position: absolute;
            left: 0;
            color: #28a745;
            font-weight: bold;
        }
        .info-box {
            background: #e3f2fd;
            border-left: 4px solid #2196f3;
            padding: 15px;
            margin: 20px 0;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🚀 Welcome to ASP Classic Language Support!</h1>
        
        <div class="info-box">
            <strong>Plugin Status:</strong> Active and working!
            <br>
            <strong>HTML Parser:</strong> Fully integrated
        </div>
        
        <h2>✨ Features Available:</h2>
        <ul class="feature-list">
            <li><strong>HTML Syntax Highlighting</strong> - All HTML tags are properly highlighted</li>
            <li><strong>Code Completion</strong> - Type &lt; to see HTML tag suggestions</li>
            <li><strong>Tag Matching</strong> - Opening and closing tags are matched</li>
            <li><strong>Code Folding</strong> - Collapse HTML blocks</li>
            <li><strong>Validation</strong> - HTML errors are detected</li>
            <li><strong>Navigation</strong> - Jump to matching tags</li>
            <li><strong>Formatting</strong> - Reformat code with IDE formatter</li>
        </ul>
        
        <h2>🧪 Try These Tests:</h2>
        <ol>
            <li>Start typing <code>&lt;di</code> and press Ctrl+Space for autocomplete</li>
            <li>Click on any opening tag to see its matching closing tag</li>
            <li>Try to create invalid HTML and see error highlighting</li>
            <li>Use Ctrl+Alt+L to reformat this document</li>
        </ol>
        
        <div class="info-box">
            <h3>Example Form:</h3>
            <form action="submit.asp" method="post">
                <label for="name">Name:</label>
                <input type="text" id="name" name="name" required>
                
                <label for="email">Email:</label>
                <input type="email" id="email" name="email" required>
                
                <button type="submit">Submit</button>
            </form>
        </div>
        
        <footer>
            <p><small>ASP Classic Language Support Plugin - HTML Parser Integration</small></p>
        </footer>
    </div>
</body>
</html>

