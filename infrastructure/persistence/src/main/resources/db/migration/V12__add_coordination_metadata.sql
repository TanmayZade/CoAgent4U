-- V12: Add metadata column to coordinations table
ALTER TABLE coordinations ADD COLUMN IF NOT EXISTS metadata_json JSONB;
