-- name: add-project-sql
-- Adds a new project record to mapcha.projects.
INSERT INTO mapcha.projects (name, description, boundary)
  VALUES (:name, :description,
          ST_MakeEnvelope(:lon_min,:lat_min,:lon_max,:lat_max,4326))
  RETURNING id, name, description, ST_AsText(boundary) AS boundary;

-- name: add-plot-sql
-- Adds a new plot record to mapcha.plots for the given project_id.
INSERT INTO mapcha.plots (project_id, center, radius)
  VALUES (:project_id, ST_SetSRID(ST_Point(:lon,:lat),4326), :radius)
  RETURNING id, project_id, ST_AsText(center) AS center, radius,
            ST_X(ST_Transform(center,3857)) AS web_mercator_x,
            ST_Y(ST_Transform(center,3857)) AS web_mercator_y;

-- name: add-sample-sql
-- Adds a new sample record to mapcha.samples for the given plot_id.
INSERT INTO mapcha.samples (plot_id, point)
  VALUES (:plot_id,
          ST_Transform(ST_SetSRID(ST_Point(:sample_x,:sample_y),3857),4326))
  RETURNING id, plot_id, ST_AsText(point) AS point;

-- name: add-sample-value-sql
-- Adds a new sample value for the given project_id.
INSERT INTO mapcha.sample_values (project_id, value)
  VALUES (:project_id, :value)
  RETURNING id, project_id, value;

-- name: add-user-sample-sql
-- Adds a new sample record for the given user_id.
INSERT INTO mapcha.user_samples (user_id, sample_id, value_id)
  VALUES (:user_id, :sample_id, :value_id)
  RETURNING *;

-- name: get-all-projects-sql
-- Returns all rows in mapcha.projects.
SELECT id, name, description, ST_AsGeoJSON(boundary) AS boundary
  FROM mapcha.projects;

-- name: get-sample-values-sql
-- Returns all rows in mapcha.sample_values with the given project_id.
SELECT id, value
  FROM mapcha.sample_values
  WHERE project_id = :project_id;

-- name: get-random-plot-sql
-- Returns a random row from mapcha.plots associated with the given project_id.
SELECT id, ST_AsGeoJSON(center) AS center, radius
  FROM mapcha.plots
  WHERE project_id = :project_id
  ORDER BY random()
  LIMIT 1;

-- name: get-sample-points-sql
-- Returns all rows in mapcha.samples associated with the given plot_id.
SELECT id, ST_AsGeoJSON(point) AS point
  FROM mapcha.samples
  WHERE plot_id = :plot_id;
