-- SQL Fix for Highcore Agency Bot
-- Run this in your Supabase SQL Editor to allow the bot to save warnings and suggestions.

-- 1. Enable INSERT for dc_warnings
ALTER TABLE dc_warnings ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow Service Role / Admin Insert" ON dc_warnings;
CREATE POLICY "Allow Service Role / Admin Insert" ON dc_warnings FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow All Select" ON dc_warnings FOR SELECT USING (true);

-- 2. Enable INSERT for dc_suggestions
ALTER TABLE dc_suggestions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow Public Suggestion Submission" ON dc_suggestions;
CREATE POLICY "Allow Public Suggestion Submission" ON dc_suggestions FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow All Select" ON dc_suggestions FOR SELECT USING (true);

-- 3. Ensure dc_suggestions has a default status
ALTER TABLE dc_suggestions ALTER COLUMN status SET DEFAULT 'Pending';

-- 4. Grant permissions to anon and authenticated roles
GRANT ALL ON TABLE dc_warnings TO anon, authenticated, service_role;
GRANT ALL ON TABLE dc_suggestions TO anon, authenticated, service_role;
