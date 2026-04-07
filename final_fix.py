import os
import re

def fix_file(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # 1. Replace reply(event, embed, true, rows) with replyEphemeral(event, embed, rows)
    # This handles both cases: with rows and without rows.
    # Pattern: PanelService\.reply\s*\(\s*([^,]+)\s*,\s*([^,]+)\s*,\s*true\s*(,\s*.*?)?\s*\)
    new_content = re.sub(
        r'PanelService\.reply\s*\(\s*([^,]+)\s*,\s*([^,]+)\s*,\s*true\s*(,\s*.*?)?\s*\)', 
        r'PanelService.replyEphemeral(\1, \2\3)', 
        content
    )
    
    # 2. Replace remaining reply(event, embed, false, rows) with reply(event, embed, rows)
    new_content = re.sub(
        r'PanelService\.reply\s*\(\s*([^,]+)\s*,\s*([^,]+)\s*,\s*false\s*(,\s*.*?)?\s*\)', 
        r'PanelService.reply(\1, \2\3)', 
        new_content
    )

    if new_content != content:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(new_content)
        return True
    return False

base_dir = r"c:\Users\omars\Downloads\highcore-bot-v14\highcore-bot\src\main\java\com\highcore\bot"
for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".java"):
            fp = os.path.join(root, file)
            if fix_file(fp):
                print(f"Fixed: {file}")
