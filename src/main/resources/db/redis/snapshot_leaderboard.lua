-- SCAN Operation Based
-- ARGV[1]: Original Leaderboard Key Pattern
-- ARGV[2]: Scan Count
-- ARGV[3]: Leaderboard Snapshot Postfix
local cursor = "0"
local moved_keys = {}

repeat
    local result = redis.call('SCAN', cursor, 'MATCH', ARGV[1], 'COUNT', ARGV[2])
    cursor = result[1]
    local keys = result[2]

    for _, key in ipairs(keys) do
        if not string.find(key, ARGV[3] .. "$") then
            local snapshot_key = key .. ARGV[3]
            redis.call('RENAME', key, snapshot_key)
            table.insert(moved_keys, snapshot_key)
        end
    end
until cursor == "0"

return moved_keys


-- -- KEYS Operation Based
-- -- ARGV[1]: Original Leaderboard Key Pattern
-- -- ARGV[2]: Leaderboard Snapshot Postfix
-- local keys = redis.call('KEYS', ARGV[1])
-- local leaderboard_snapshot_keys = {}
--
-- for _, leaderboard_key in ipairs(keys) do
--     local leaderboard_snapshot_key = leaderboard_key .. ARGV[2]
--     redis.call('RENAME', leaderboard_key, leaderboard_snapshot_key)
--     table.insert(leaderboard_snapshot_keys, leaderboard_snapshot_key)
-- end
--
-- return leaderboard_snapshot_keys
