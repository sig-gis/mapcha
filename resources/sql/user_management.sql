-- name: find-user-info-sql
-- Returns the identity, password, roles, and reset_key associated with the provided email.
SELECT identity, password, array_agg(role) AS roles, reset_key
  FROM mapcha.users
  INNER JOIN mapcha.user_roles USING (identity)
  WHERE identity = :email
  GROUP BY identity, password;

-- name: add-user-sql
-- Adds a new user to the database.
INSERT INTO mapcha.users (identity, password)
  VALUES (:email, :password)
  RETURNING identity, password;

-- name: add-user-role-sql
-- Assigns a new role to the given user.
INSERT INTO mapcha.user_roles (identity, role)
  VALUES (:email, :role)
  RETURNING identity, role;

-- name: set-user-email-sql
-- Resets the email for the given user. This cascades to mapcha.user_roles and mapcha.user_reports as well.
UPDATE mapcha.users
  SET identity = :new_email
  WHERE identity = :old_email
  RETURNING identity;

-- name: set-user-password-sql
-- Sets the password for the given user. If one already exists, it is replaced.
UPDATE mapcha.users
  SET password = :password
  WHERE identity = :email
  RETURNING identity, password;

-- name: find-user-reports-sql
-- Returns all reports associated with the provided email.
SELECT *
  FROM mapcha.user_reports
  WHERE identity = :email;

-- name: add-user-report-sql
-- Adds a new report for the given user.
INSERT INTO mapcha.user_reports (identity, address, longitude, latitude,
                               fire_risk_mean, fire_risk_stddev,
                               fire_hazard_mean, fire_hazard_stddev,
                               fire_weather_mean, fire_weather_stddev,
                               combined_score, cost)
  VALUES (:email, :address, :longitude, :latitude,
          :fire_risk_mean, :fire_risk_stddev,
          :fire_hazard_mean, :fire_hazard_stddev,
          :fire_weather_mean, :fire_weather_stddev,
          :combined_score, :cost)
  RETURNING *;

-- name: set-password-reset-key-sql
-- Sets the password reset key for the given user. If one already exists, it is replaced.
UPDATE mapcha.users
  SET reset_key = :reset_key
  WHERE identity = :email
  RETURNING identity, reset_key;
