CREATE TABLE application_attachments (
    id serial NOT NULL PRIMARY KEY,
    catAppId integer DEFAULT NULL,
    formMapId integer DEFAULT NULL,
    modifierUserId varchar(255) NOT NULL,
    filename varchar(255) NOT NULL,
    data bytea NOT NULL,
    start timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT application_attachments_ibfk_1 FOREIGN KEY (catAppId) REFERENCES catalogue_item_application (id),
    CONSTRAINT application_attachments_ibfk_2 FOREIGN KEY (formMapId) REFERENCES application_form_item_map (id),
    UNIQUE (catAppId, formMapId)
);
--;;