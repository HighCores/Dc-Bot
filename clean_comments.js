const fs = require('fs');
const path = require('path');

function walk(dir, callback) {
    fs.readdirSync(dir).forEach(f => {
        let dirPath = path.join(dir, f);
        let isDirectory = fs.statSync(dirPath).isDirectory();
        isDirectory ? walk(dirPath, callback) : callback(dirPath);
    });
}

function processFile(filePath) {
    if (!filePath.endsWith('.java')) return;
    let content = fs.readFileSync(filePath, 'utf8');
    
    let lines = content.split(/\r?\n/);
    let newLines = [];
    
    for (let i = 0; i < lines.length; i++) {
        let line = lines[i];
        let trimmed = line.trim();
        
        // Match line comments
        if (trimmed.startsWith('//')) {
            // Keep headers and section markers
            let isHeader = trimmed.includes('===') || 
                           trimmed.includes('---') || 
                           trimmed.match(/^\/\/\s*[A-Z_0-9\s]+$/) || 
                           trimmed.match(/^\/\/\s*[\u2728\uD83E\uDD16]/); // Sparkles/Robot emojis
            
            // Remove the duplicate block user pointed out
            if (trimmed.includes('Deep UI V2 Extraction')) {
                continue; // Always delete
            }
            
            if (!isHeader && trimmed.length > 3) {
                // Check if it's commented out code
                if (trimmed.includes(';') || trimmed.includes('{') || trimmed.includes('}')) {
                    // Let's keep commented out code just in case? No, user said delete all human-generated comments.
                    // Wait, sometimes people comment out code. The user said "All code comments are human-generated and not educational".
                    continue;
                }
                continue;
            }
        }
        
        newLines.push(line);
    }
    
    fs.writeFileSync(filePath, newLines.join('\n'));
}

walk('src/main/java', processFile);
console.log('Done cleaning comments.');
