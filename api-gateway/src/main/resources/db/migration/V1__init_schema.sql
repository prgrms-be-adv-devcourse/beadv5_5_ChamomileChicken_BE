CREATE TABLE IF NOT EXISTS gateway_whitelist (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    method       VARCHAR(10)  NOT NULL,
    path_pattern VARCHAR(255) NOT NULL,
    description  VARCHAR(255),
    enabled      BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS gateway_route_policy (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    method        VARCHAR(10)  NOT NULL,
    path_pattern  VARCHAR(255) NOT NULL,
    allowed_roles VARCHAR(255) NOT NULL,
    description   VARCHAR(255),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE
    );
