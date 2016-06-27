-- name: find-user-info-sql
-- Returns all of the user fields associated with the provided email.
SELECT id, email AS identity, password, role, reset_key
  FROM mapcha.users
  WHERE email = :email;

-- name: add-user-sql
-- Adds a new user to the database.
INSERT INTO mapcha.users (email, password, role)
  VALUES (:email, :password, :role)
  RETURNING email, password, role;

-- name: set-user-email-sql
-- Resets the email for the given user.
UPDATE mapcha.users
  SET email = :new_email
  WHERE email = :old_email
  RETURNING email;

-- name: set-user-password-sql
-- Resets the password for the given user.
UPDATE mapcha.users
  SET password = :password
  WHERE email = :email
  RETURNING email, password;

-- name: find-user-reports-sql
-- Returns all reports associated with the provided email.
SELECT *
  FROM mapcha.user_reports
  WHERE email = :email;

-- name: add-user-report-sql
-- Adds a new report for the given user.
INSERT INTO mapcha.user_reports (email, address, longitude, latitude,
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

-- name: add-user-sample-sql
-- Adds a new sample record for the given user.
INSERT INTO mapcha.user_samples (user_id, sample_id, value)
  VALUES (:user_id, :sample_id, :value)
  RETURNING *;

-- name: set-password-reset-key-sql
-- Sets the password reset key for the given user. If one already exists, it is replaced.
UPDATE mapcha.users
  SET reset_key = :reset_key
  WHERE email = :email
  RETURNING email, reset_key;
