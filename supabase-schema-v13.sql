-- ============================================
-- v13 - Run this in Supabase SQL Editor
-- ============================================

-- Color roles (bot creates these on startup)
CREATE TABLE IF NOT EXISTS dc_color_roles (
    id BIGSERIAL PRIMARY KEY,
    guild_id TEXT NOT NULL,
    role_id TEXT NOT NULL,
    color_name TEXT NOT NULL,
    color_hex TEXT NOT NULL,
    position INT DEFAULT 0,
    UNIQUE(guild_id, color_name)
);

-- Reputation points
CREATE TABLE IF NOT EXISTS dc_reputation (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    rep_points INT DEFAULT 0,
    UNIQUE(user_id, guild_id)
);

-- Rep cooldown tracking
CREATE TABLE IF NOT EXISTS dc_rep_cooldown (
    id BIGSERIAL PRIMARY KEY,
    giver_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    last_given_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(giver_id, guild_id)
);

-- Vito (special currency/reputation)
CREATE TABLE IF NOT EXISTS dc_vitos (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    vitos INT DEFAULT 0,
    UNIQUE(user_id, guild_id)
);

-- Suggestions
CREATE TABLE IF NOT EXISTS dc_suggestions (
    id BIGSERIAL PRIMARY KEY,
    guild_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    user_name TEXT,
    message_id TEXT,
    channel_id TEXT,
    content TEXT NOT NULL,
    status TEXT DEFAULT 'pending',
    reviewed_by TEXT,
    review_note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Profile titles (for specific role)
CREATE TABLE IF NOT EXISTS dc_titles (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    title TEXT DEFAULT '',
    UNIQUE(user_id, guild_id)
);

-- Voice XP tracking (for leveling)
ALTER TABLE dc_levels ADD COLUMN IF NOT EXISTS voice_xp INT DEFAULT 0;
ALTER TABLE dc_levels ADD COLUMN IF NOT EXISTS voice_minutes INT DEFAULT 0;
