-- Calculates the various fire weather scores for a lon/lat point within a buffer of size radius.
WITH coordinate           AS (SELECT ST_Transform(ST_SetSRID(ST_Point(:lon, :lat), 4326), 2163) AS geom),
     ecocode_ffwis        AS (SELECT avg_ffwi, stddev_ffwi, percentile_min AS local_min, percentile_max AS local_max
                                FROM coordinate AS c
                                INNER JOIN eco.ecocodes AS e ON ST_Contains(e.geom, c.geom)
                                INNER JOIN weather.raws_station_extreme_weather_percentiles_by_ecocode USING (ecocode)
                                WHERE percentile = 99),
     buffer_stations      AS (SELECT wrcc, ST_Distance(c.geom, s.geom) AS distance
                                FROM coordinate AS c
                                INNER JOIN weather.raws_points AS s ON ST_DWithin(c.geom, s.geom, :radius)),
     station_ffwis        AS (SELECT avg_ffwi, stddev_ffwi, (1/distance^:power_factor)/sum(1/distance^:power_factor) OVER () AS weight
                                FROM buffer_stations
                                INNER JOIN weather.raws_station_extreme_weather_percentiles USING (wrcc)
                                WHERE percentile = 99
                                  AND distance > 0),
     merged_station_ffwis AS (SELECT sum(avg_ffwi*weight) AS avg_ffwi,
                                     sum(stddev_ffwi*weight) AS stddev_ffwi,
                                     min(avg_ffwi) AS local_min,
                                     max(avg_ffwi) AS local_max
                                FROM station_ffwis),
     scaled_ecocode_ffwis AS (SELECT 100*(avg_ffwi - global_min)/(global_max - global_min) AS global_mean,
                                     100*stddev_ffwi/(global_max - global_min) AS global_stddev,
                                 CASE WHEN local_max = local_min THEN null ELSE 100*(avg_ffwi - local_min)/(local_max - local_min) END AS local_mean,
                                 CASE WHEN local_max = local_min THEN null ELSE 100*stddev_ffwi/(local_max - local_min) END AS local_stddev
                                FROM ecocode_ffwis
                                CROSS JOIN (VALUES (21.9712616489168, 52.5204575807574)) AS scale (global_min, global_max)),
     scaled_station_ffwis AS (SELECT 100*(avg_ffwi - global_min)/(global_max - global_min) AS global_mean,
                                     100*stddev_ffwi/(global_max - global_min) AS global_stddev,
                                 CASE WHEN local_max = local_min THEN null ELSE 100*(avg_ffwi - local_min)/(local_max - local_min) END AS local_mean,
                                 CASE WHEN local_max = local_min THEN null ELSE 100*stddev_ffwi/(local_max - local_min) END AS local_stddev
                                FROM merged_station_ffwis
                                CROSS JOIN (VALUES (8.63254006270451, 117.345385607096)) AS scale (global_min, global_max))
  SELECT CASE WHEN s.global_mean IS NULL THEN 'ecocode'       ELSE 'station'       END AS source,
         CASE WHEN s.global_mean IS NULL THEN e.global_mean   ELSE s.global_mean   END AS global_mean,
         CASE WHEN s.global_mean IS NULL THEN e.global_stddev ELSE s.global_stddev END AS global_stddev,
         CASE WHEN s.global_mean IS NULL THEN e.local_mean    ELSE s.local_mean    END AS local_mean,
         CASE WHEN s.global_mean IS NULL THEN e.local_stddev  ELSE s.local_stddev  END AS local_stddev,
         CASE WHEN s.global_mean IS NULL THEN eh.midpoint     ELSE sh.midpoint     END AS midpoint,
         CASE WHEN s.global_mean IS NULL THEN eh.width        ELSE sh.width        END AS width,
         CASE WHEN s.global_mean IS NULL THEN eh.percent      ELSE sh.percent      END AS percent
    FROM scaled_ecocode_ffwis AS e
    CROSS JOIN scaled_station_ffwis AS s
    CROSS JOIN eco.ca99_histogram AS eh
    LEFT JOIN weather.ca99_histogram AS sh USING (bin);
