
begin ;
  drop table userconnection cascade ;
  drop table photos cascade ;
  drop table photo_albums cascade ;
commit ;

--
-- this is used by Spring Social to store information about the Flickr-authorized connections
--
CREATE TABLE userconnection
(
  userid character varying(255) NOT NULL,
  providerid character varying(255) NOT NULL,
  provideruserid character varying(255) NOT NULL,
  rank integer NOT NULL,
  displayname character varying(255),
  profileurl character varying(512),
  imageurl character varying(512),
  accesstoken character varying(255) NOT NULL,
  secret character varying(255),
  refreshtoken character varying(255),
  expiretime bigint,
  CONSTRAINT userconnection_pkey PRIMARY KEY (userid , providerid , provideruserid )
) ;

-- DROP INDEX userconnectionrank;

CREATE UNIQUE INDEX userconnectionrank
  ON userconnection
  USING btree
  (userid COLLATE pg_catalog."default" , providerid COLLATE pg_catalog."default" , rank );


--
-- used to hold the idea of photo sets
--
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
) ;


--
-- used to hold individual photos
--
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
  CONSTRAINT photo_album_fk FOREIGN KEY (album_id)
      REFERENCES photo_albums (album_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT photo_album_photo UNIQUE (album_id , photo_id )
);