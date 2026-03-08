DROP TABLE IF EXISTS image_recycle_bin;
CREATE TABLE IF NOT EXISTS image_recycle_bin (
     id INTEGER PRIMARY KEY AUTOINCREMENT,
     path TEXT NOT NULL,
     retry_count INTEGER NOT NULL DEFAULT 0,
     deleted_at DATETIME NOT NULL
);
