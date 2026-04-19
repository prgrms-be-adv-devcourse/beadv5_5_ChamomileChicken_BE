TRUNCATE TABLE gateway_route_policy;
TRUNCATE TABLE gateway_whitelist;

INSERT INTO gateway_whitelist (method, path_pattern, description) VALUES
    ('POST', '/api/v1/auth/login',              '로그인'),
    ('POST', '/api/v1/auth/reissue',            '토큰 재발급'),
    ('POST', '/api/v1/users/register',          '회원가입'),
    ('POST', '/api/v1/users/email-check',       '이메일 중복 확인'),
    ('POST', '/api/v1/email/**',                '이메일 인증'),
    ('GET',  '/api/v1/products',                '상품 목록 조회'),
    ('GET',  '/api/v1/products/*',              '상품 상세 조회'),
    ('GET',  '/api/v1/products/*/schedules',    '상품 일정 목록 조회'),
    ('GET',  '/api/v1/products/*/availability', '스케줄 예약 가능 여부'),
    ('GET',  '/api/v1/products/*/reviewList',   '상품 리뷰 목록 조회'),
    ('GET',  '/api/v1/products/*/reviews/*',    '리뷰 단건 조회');

INSERT INTO gateway_route_policy (method, path_pattern, allowed_roles, description) VALUES
    ('POST',   '/api/v1/products',               'SELLER,ADMIN', '상품 등록'),
    ('PUT',    '/api/v1/products/*',             'SELLER,ADMIN', '상품 수정'),
    ('DELETE', '/api/v1/products/*',             'SELLER,ADMIN', '상품 삭제'),
    ('POST',   '/api/v1/products/*/schedules',   'SELLER,ADMIN', '일정 등록'),
    ('PUT',    '/api/v1/products/*/schedules/*', 'SELLER,ADMIN', '일정 수정'),
    ('DELETE', '/api/v1/products/*/schedules/*', 'SELLER,ADMIN', '일정 삭제'),
    ('GET',    '/api/v1/settlements',            'SELLER,ADMIN', '정산 목록 조회'),
    ('GET',    '/api/v1/settlements/ready',      'SELLER,ADMIN', 'READY 정산 목록 조회'),
    ('GET',    '/api/v1/settlements/*',          'SELLER,ADMIN', '정산 단건 조회'),
    ('GET',    '/api/v1/admins/**',              'ADMIN', '어드민 조회'),
    ('POST',   '/api/v1/admins/**',              'ADMIN', '어드민 등록'),
    ('PATCH',  '/api/v1/admins/**',              'ADMIN', '어드민 수정'),
    ('DELETE', '/api/v1/admins/**',              'ADMIN', '어드민 삭제');
