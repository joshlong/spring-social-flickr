
CREATE TABLE photo_albums
(
  id serial NOT NULL,
  user_id character varying(300) NOT NULL,
  count_photos integer NOT NULL,
  count_videos integer NOT NULL,
  title character varying(300) NOT NULL,
  description character varying(300) NOT NULL,
  album_id character varying(100) NOT NULL,
  url character varying(300),
  CONSTRAINT photo_albums_pkey PRIMARY KEY (id ),
  CONSTRAINT photo_albums_album_id_key UNIQUE (album_id )
);

CREATE TABLE photos
(
  id serial NOT NULL,
  album_id character varying(100) NOT NULL,
  photo_id character varying(300) NOT NULL,
  title character varying(300),
  url character varying(300) NOT NULL,
  thumb_url character varying(300) NOT NULL,
  downloaded date,
  is_primary boolean not null default false ,
  comments character varying(300),
  CONSTRAINT photos_pkey PRIMARY KEY (id ),
  CONSTRAINT photo_album_fk FOREIGN KEY (album_id) REFERENCES photo_albums (album_id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT photo_album_photo UNIQUE (album_id , photo_id )
);