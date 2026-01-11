-- Add generic provider columns to gs_bookings table
-- These columns work for any provider (Dhruva/ISOCS, Leafspace, ISRO, etc.)

ALTER TABLE gs_bookings ADD COLUMN IF NOT EXISTS provider_satellite_id VARCHAR(100);
ALTER TABLE gs_bookings ADD COLUMN IF NOT EXISTS provider_gs_id VARCHAR(100);
ALTER TABLE gs_bookings ADD COLUMN IF NOT EXISTS provider_contact_id VARCHAR(100);
ALTER TABLE gs_bookings ADD COLUMN IF NOT EXISTS provider_booking_id VARCHAR(100);
ALTER TABLE gs_bookings ADD COLUMN IF NOT EXISTS provider_metadata JSONB;
ALTER TABLE gs_bookings ADD COLUMN IF NOT EXISTS max_elevation DOUBLE PRECISION;

-- Create index on the provider booking ID for quick lookups
CREATE INDEX IF NOT EXISTS idx_gs_bookings_provider_booking_id
    ON gs_bookings(provider_booking_id)
    WHERE provider_booking_id IS NOT NULL;

-- Create index on provider + booking_id for provider-specific queries
CREATE INDEX IF NOT EXISTS idx_gs_bookings_provider_lookup
    ON gs_bookings(provider, provider_booking_id)
    WHERE provider_booking_id IS NOT NULL;

COMMENT ON COLUMN gs_bookings.provider_satellite_id IS 'Provider satellite ID (e.g., ISOCS satellite_id, Leafspace sat_id)';
COMMENT ON COLUMN gs_bookings.provider_gs_id IS 'Provider ground station ID';
COMMENT ON COLUMN gs_bookings.provider_contact_id IS 'Provider contact/visibility window ID';
COMMENT ON COLUMN gs_bookings.provider_booking_id IS 'Provider booking ID returned after reservation';
COMMENT ON COLUMN gs_bookings.provider_metadata IS 'Provider-specific metadata as JSON (activity scopes, etc.)';
COMMENT ON COLUMN gs_bookings.max_elevation IS 'Maximum elevation angle in degrees for the pass';
