import os

file_path = r"c:\Users\omars\Downloads\highcore-bot-v14\highcore-bot\src\main\java\com\highcore\bot\services\SettingSyncService.java"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

new_content = content.replace("Neural Sync Service started", "Global Settings Sync started")
new_content = new_content.replace("Neural Sync loop error", "Global Settings Sync error")

if new_content != content:
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(new_content)
    print("Humanized SettingSyncService.java")
else:
    print("No changes needed for SettingSyncService.java")
