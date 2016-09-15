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
-- Returns all rows in mapcha.projects for which archived = false.
SELECT id, name, description, ST_AsGeoJSON(boundary) AS boundary
  FROM mapcha.projects
  WHERE archived = false
  ORDER BY id;

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
  WHERE project_id = :project_id
  ORDER BY id;

-- name: get-random-plot-sql
-- Returns a random non-flagged row from mapcha.plots with the given project_id.
SELECT id, ST_AsGeoJSON(center) AS center, radius
  FROM mapcha.plots
  WHERE project_id = :project_id
    AND flagged = false
  ORDER BY random()
  LIMIT 1;

-- name: get-random-plot-by-min-analyses-sql
-- Returns a random non-flagged row from mapcha.plots with the given project_id.
SELECT id, center, radius
  FROM (SELECT id, ST_AsGeoJSON(center) AS center, radius,
               analyses, min(analyses) OVER () AS min_analyses
          FROM mapcha.plots
          WHERE project_id = :project_id
            AND flagged = false) AS foo
  WHERE analyses = min_analyses
  ORDER BY random()
  LIMIT 1;

-- name: get-sample-points-sql
-- Returns all rows in mapcha.samples associated with the given plot_id.
SELECT id, ST_AsGeoJSON(point) AS point
  FROM mapcha.samples
  WHERE plot_id = :plot_id;

-- name: increment-plot-analyses-sql
-- Adds 1 to the analyses column in mapcha.plots for the given plot_id.
UPDATE mapcha.plots SET analyses = analyses + 1 WHERE id = :plot_id
  RETURNING analyses;

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

-- name: flag-plot-sql
-- Sets the flagged field to true in mapcha.plots for the given plot_id.
UPDATE mapcha.plots
  SET flagged = true
  WHERE id = :plot_id
  RETURNING id;

-- name: dump-project-aggregate-data-sql
-- Returns an overview of the sample data entered thus far for the given project_id.
WITH plot_data AS (SELECT plt.id AS plot_id, ST_X(center) AS center_lon,
                          ST_Y(center) AS center_lat, radius AS radius_m,
                          flagged, sample_id, user_id, value_id, value
                     FROM mapcha.plots AS plt
                     INNER JOIN mapcha.samples AS smp ON plt.id = plot_id
                     INNER JOIN mapcha.user_samples ON smp.id = sample_id
                     INNER JOIN mapcha.sample_values AS val ON value_id = val.id
                     WHERE plt.project_id = :project_id),
     plot_overview AS (SELECT plot_id, center_lon, center_lat, radius_m, flagged,
                              count(sample_id) AS sample_points,
                              count(distinct user_id) AS user_assignments
                         FROM plot_data
                         GROUP BY plot_id, center_lon, center_lat,
                                  radius_m, flagged),
     plot_values AS (SELECT plot_id, value,
                            (100.0 * count(value_id) /
                              (sample_points*user_assignments))::float AS percent
                       FROM plot_data
                       INNER JOIN plot_overview USING (plot_id)
                       GROUP BY plot_id, sample_points, user_assignments, value
                       ORDER BY plot_id)
  SELECT *
    FROM plot_overview
    INNER JOIN plot_values USING (plot_id);
