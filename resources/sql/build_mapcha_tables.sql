CREATE SCHEMA mapcha;

CREATE TABLE mapcha.projects (
  id          serial primary key,
  name        text not null,
  description text,
  boundary    geometry(Polygon,4326),
  archived    boolean default false
);

CREATE TABLE mapcha.plots (
  id         serial primary key,
  project_id integer not null references mapcha.projects (id) on delete cascade on update cascade,
  center     geometry(Point,4326),
  radius     double precision not null,
  flagged    boolean default false
);

CREATE INDEX mapcha_plots_project_id ON mapcha.plots (project_id);

CREATE TABLE mapcha.samples (
  id      serial primary key,
  plot_id integer not null references mapcha.plots (id) on delete cascade on update cascade,
  point   geometry(Point,4326)
);

CREATE INDEX mapcha_samples_plot_id ON mapcha.samples (plot_id);

CREATE TABLE mapcha.sample_values (
  id         serial primary key,
  project_id integer not null references mapcha.projects (id) on delete cascade on update cascade,
  value      text not null
  color      text not null;
  image      text;
);

CREATE INDEX mapcha_sample_values_project_id ON mapcha.sample_values (project_id);

CREATE TABLE mapcha.imagery (
  id          serial primary key,
  title       text not null,
  date        date,
  url         text not null,
  attribution text
);

CREATE TABLE mapcha.users (
  id        serial primary key,
  email     text not null,
  password  text not null,
  role      text not null,
  reset_key text,
  ip_addr   inet
);

CREATE INDEX mapcha_users_email ON mapcha.users (email);

CREATE TABLE mapcha.user_samples (
  id           serial primary key,
  user_id      integer not null references mapcha.users (id) on delete cascade on update cascade,
  sample_id    integer not null references mapcha.samples (id) on delete cascade on update cascade,
  value_id     integer not null references mapcha.sample_values (id) on delete cascade on update cascade,
  imagery_id   integer not null references mapcha.imagery (id) on delete cascade on update cascade,
  date         date
);

CREATE INDEX mapcha_user_samples_user_id ON mapcha.user_samples (user_id);
CREATE INDEX mapcha_user_samples_sample_id ON mapcha.user_samples (sample_id);
