-- Migration: Add duration_minutes to coordinations table
ALTER TABLE coordinations ADD COLUMN IF NOT EXISTS duration_minutes INTEGER NOT NULL DEFAULT 60;
