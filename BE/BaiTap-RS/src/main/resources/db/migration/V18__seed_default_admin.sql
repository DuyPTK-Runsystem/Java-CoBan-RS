-- Plan 048: the technical administrator must exist after a clean migration.
-- The stored value is a BCrypt hash of the fixture password "admin".
INSERT INTO app_user (user_name, password, created_at)
SELECT 'admin', '$2a$10$ScW2K80xVVEkhJtwudgaXOa/x39XVKzJUIE2WMOBaWxlPADdtKjdu',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE user_name = 'admin'
);

INSERT INTO user_role (user_id, role_id, assigned_at)
SELECT app_user.user_id, role.role_id, CURRENT_TIMESTAMP
FROM app_user
JOIN role ON role.code = 'ADMIN'
WHERE app_user.user_name = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM user_role existing_user_role
      WHERE existing_user_role.user_id = app_user.user_id
        AND existing_user_role.role_id = role.role_id
  );
