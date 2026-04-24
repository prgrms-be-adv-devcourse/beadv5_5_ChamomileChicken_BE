INSERT INTO gateway_whitelist (id, method, path_pattern, description)
VALUES (gen_random_uuid(),'GET', '/api/v1/auth/report-theft', '토큰 탈취 신고 (본인 아님)');
