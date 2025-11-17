-- ============================================================
-- VEHICLE TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS vehicle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL
);

-- ============================================================
-- CLEANER PROFESSIONAL TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS cleaner_professional (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    vehicle_id BIGINT NOT NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(id)
        ON DELETE CASCADE
);

-- ============================================================
-- BOOKING TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    duration_in_hours INT NOT NULL,
    requested_cleaner_count INT NOT NULL
);

-- ============================================================
-- BOOKING-CLEANER MAPPING TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS booking_cleaner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    cleaner_id BIGINT NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES booking(id)
        ON DELETE CASCADE,
    FOREIGN KEY (cleaner_id) REFERENCES cleaner_professional(id)
        ON DELETE CASCADE
);

-- ============================================================
-- ADD UNIQUE CONSTRAINT SAFELY (booking_id, cleaner_id)
-- ============================================================
SET @exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_name = 'booking_cleaner'
      AND constraint_name = 'uq_booking_cleaner'
);

SET @sql := IF(
    @exists = 0,
    'ALTER TABLE booking_cleaner ADD CONSTRAINT uq_booking_cleaner UNIQUE (booking_id, cleaner_id)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- AVAILABILITY BLOCKS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS availability_blocks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cleaner_id BIGINT NOT NULL,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    block_type VARCHAR(20) NOT NULL,
    booking_id BIGINT NULL,
    FOREIGN KEY (cleaner_id) REFERENCES cleaner_professional(id)
        ON DELETE CASCADE
);

-- ============================================================
-- ADD booking_id COLUMN SAFELY (only if missing)
-- ============================================================
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_name = 'availability_blocks'
      AND column_name = 'booking_id'
);

SET @sql_col := IF(
    @col_exists = 0,
    'ALTER TABLE availability_blocks ADD COLUMN booking_id BIGINT NULL',
    'SELECT 1'
);

PREPARE stmt_col FROM @sql_col;
EXECUTE stmt_col;
DEALLOCATE PREPARE stmt_col;

-- ============================================================
-- ADD FOREIGN KEY SAFELY (booking_id → booking.id)
-- ============================================================
SET @fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_name = 'availability_blocks'
      AND constraint_name = 'fk_block_booking'
);

SET @sql_fk := IF(
    @fk_exists = 0,
    'ALTER TABLE availability_blocks
         ADD CONSTRAINT fk_block_booking
         FOREIGN KEY (booking_id) REFERENCES booking(id)
         ON DELETE CASCADE',
    'SELECT 1'
);

PREPARE stmt_fk FROM @sql_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;

-- ============================================================
-- ADD UNIQUE CONSTRAINT SAFELY FOR BLOCKS
-- ============================================================
SET @exists2 := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_name = 'availability_blocks'
      AND constraint_name = 'uq_cleaner_block'
);

SET @sql2 := IF(
    @exists2 = 0,
    'ALTER TABLE availability_blocks
        ADD CONSTRAINT uq_cleaner_block
        UNIQUE (cleaner_id, start_datetime, end_datetime, block_type)',
    'SELECT 1'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
