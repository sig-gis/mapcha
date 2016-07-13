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
INSERT INTO mapcha.sample_values (project_id, value, color, image)
  VALUES (:project_id, :value, :color, :image)
  RETURNING id, project_id, value, color, image;

-- name: get-all-projects-sql
-- Returns all rows in mapcha.projects.
SELECT id, name, description, ST_AsGeoJSON(boundary) AS boundary
  FROM mapcha.projects;

-- name: get-project-info-sql
-- Returns all of the properties of the project matching project_id.
WITH plots      AS (SELECT count(id) AS num_plots
                      FROM mapcha.plots
                      WHERE project_id = :project_id),
     first_plot AS (SELECT id AS plot_id, radius
                      FROM mapcha.plots
                      WHERE project_id = :project_id
                      LIMIT 1),
     samples    AS (SELECT count(id) AS num_samples
                      FROM first_plot
                      INNER JOIN mapcha.samples USING (plot_id))
  SELECT name, description, num_plots, radius, num_samples,
         ST_XMin(boundary) AS lon_min,
         ST_XMax(boundary) AS lon_max,
         ST_YMin(boundary) AS lat_min,
         ST_YMax(boundary) AS lat_max
    FROM mapcha.projects
    CROSS JOIN plots
    CROSS JOIN first_plot
    CROSS JOIN samples
    WHERE id = :project_id;

-- name: get-sample-values-sql
-- Returns all rows in mapcha.sample_values with the given project_id.
SELECT id, value, color, image
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

-- name: add-user-sample-sql
-- Adds a new sample record for the given user_id.
INSERT INTO mapcha.user_samples (user_id, sample_id, value_id, imagery_id, date)
  VALUES (:user_id, :sample_id, :value_id, :imagery_id, CURRENT_DATE)
  RETURNING *;

-- name: archive-project-sql
-- Sets the archived field to true in mapcha.projects for the given project_id.
UPDATE mapcha.projects
  SET archived = true
  WHERE id = :project_id
  RETURNING id;

-- name: revive-project-sql
-- Sets the archived field to false in mapcha.projects for the given project_id.
UPDATE mapcha.projects
  SET archived = false
  WHERE id = :project_id
  RETURNING id;
