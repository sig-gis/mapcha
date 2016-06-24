-- Calculates the various fire risk scores for a lon/lat point within a buffer of size radius.
WITH coordinate       AS (SELECT ST_Transform(ST_SetSRID(ST_Point(:lon, :lat), 4326), 2163) AS geom),
     buffer           AS (SELECT ST_Buffer(geom, :radius) AS geom
                            FROM coordinate),
     clipped_raster   AS (SELECT ST_Union(ST_Clip(rast, geom)) AS rast
                            FROM fire_risk.mean_ca
                            CROSS JOIN buffer
                            WHERE ST_Intersects(rast, geom)),
     layer_pixels     AS (SELECT layer, ST_PixelAsCentroids(rast, band) AS pixel
                            FROM clipped_raster
                            CROSS JOIN (VALUES ('mean_ca_clim', 1),
                                               ('mean_ca_der' , 2),
                                               ('mean_ca_full', 3)) AS v (layer, band)),
     pixel_distances  AS (SELECT layer, (pixel).val AS val, ST_Distance(geom, (pixel).geom) AS distance
                            FROM layer_pixels
                            CROSS JOIN coordinate),
     pixel_weights    AS (SELECT layer, val, (1/distance^:power_factor)/sum(1/distance^:power_factor) OVER (PARTITION BY layer) AS weight
                            FROM pixel_distances
                            WHERE distance > 0),
     layer_stats      AS (SELECT layer, min(val) AS local_min, max(val) AS local_max, sum(val*weight) AS idw
                            FROM pixel_weights
                            GROUP BY layer),
     scaled_stats     AS (SELECT layer,
                                 100*(idw - global_min)/(global_max - global_min) AS global_idw,
                                 CASE WHEN local_max = local_min THEN null ELSE 100*(idw - local_min)/(local_max - local_min) END AS local_idw
                            FROM layer_stats
                            INNER JOIN (VALUES ('mean_ca_clim', 0, 95),
                                               ('mean_ca_der' , 0, 94),
                                               ('mean_ca_full', 0, 93)) AS scale (layer, global_min, global_max) USING (layer)),
     merged_stats     AS (SELECT avg(global_idw) AS global_mean,
                                 stddev(global_idw) AS global_stddev,
                                 avg(local_idw) AS local_mean,
                                 stddev(local_idw) AS local_stddev
                            FROM scaled_stats),
     raster_histogram AS (SELECT ((bin).min + (bin).max)/2 AS midpoint, ((bin).max - (bin).min) AS width, (bin).percent
                            FROM (SELECT ST_Histogram(rast, 4, 10) AS bin
                                    FROM clipped_raster) AS histogram),
     scaled_histogram AS (SELECT 100*(midpoint - global_min)/(global_max - global_min) AS midpoint,
                                 100*width/(global_max - global_min) AS width,
                                 (100*percent)::integer AS percent
                            FROM raster_histogram
                            CROSS JOIN (VALUES (0, 91)) AS scale (global_min, global_max))
  SELECT *
    FROM merged_stats
    CROSS JOIN scaled_histogram;
