-- Calculates the various fire hazard scores for a lon/lat point within a buffer of size radius.
WITH coordinate       AS (SELECT ST_Transform(ST_SetSRID(ST_Point(:lon, :lat), 4326), 26910) AS geom),
     buffer           AS (SELECT ST_Buffer(geom, :radius) AS geom
                            FROM coordinate),
     clipped_raster   AS (SELECT ST_Union(ST_Clip(rast, geom)) AS rast
                            FROM fire_hazard.summary_stats -- 21 band raster
                            CROSS JOIN buffer
                            WHERE ST_Intersects(rast, geom)),
     layer_pixels     AS (SELECT layer, ST_PixelAsCentroids(rast, band) AS pixel
                            FROM clipped_raster
                            CROSS JOIN (VALUES ('favg975', 18),
                                               ('fstd975', 19)) AS v (layer, band)),
     pixel_distances  AS (SELECT layer, (pixel).val AS val, ST_Distance(geom, (pixel).geom) AS distance
                            FROM layer_pixels
                            CROSS JOIN coordinate),
     pixel_weights    AS (SELECT layer, val, (1/distance^:power_factor)/sum(1/distance^:power_factor) OVER (PARTITION BY layer) AS weight
                            FROM pixel_distances
                            WHERE distance > 0),
     layer_stats      AS (SELECT layer, min(val), max(val), sum(val*weight) AS idw
                            FROM pixel_weights
                            GROUP BY layer),
     merged_stats     AS (SELECT avg.min AS local_min,
                                 avg.max AS local_max,
                                 avg.idw AS mean,
                                 std.idw AS stddev
                            FROM layer_stats AS avg
                            CROSS JOIN layer_stats AS std
                            WHERE avg.layer = 'favg975'
                              AND std.layer = 'fstd975'),
     scaled_stats     AS (SELECT 100*(mean - global_min)/(global_max - global_min) AS global_mean,
                                 100*stddev/(global_max - global_min) AS global_stddev,
                                 CASE WHEN local_max = local_min THEN null ELSE 100*(mean - local_min)/(local_max - local_min) END AS local_mean,
                                 CASE WHEN local_max = local_min THEN null ELSE 100*stddev/(local_max - local_min) END AS local_stddev
                            FROM merged_stats
                            CROSS JOIN (VALUES (0, 54.1798934936523)) AS scale (global_min, global_max)),
     raster_histogram AS (SELECT ((bin).min + (bin).max)/2 AS midpoint, ((bin).max - (bin).min) AS width, (bin).percent
                            FROM (SELECT ST_Histogram(rast, 18, 10) AS bin
                                    FROM clipped_raster) AS histogram),
     scaled_histogram AS (SELECT 100*(midpoint - global_min)/(global_max - global_min) AS midpoint,
                                 100*width/(global_max - global_min) AS width,
                                 (100*percent)::integer AS percent
                            FROM raster_histogram
                            CROSS JOIN (VALUES (0, 54.1798934936523)) AS scale (global_min, global_max))
  SELECT *
    FROM scaled_stats
    CROSS JOIN scaled_histogram;
