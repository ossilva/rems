CREATE TYPE scope AS ENUM ('private', 'public');
--;;
CREATE TYPE itemtype AS ENUM ('text','texta','label','license','attachment','referee','checkbox','dropdown','date');
--;;
CREATE TYPE approval_status AS ENUM ('created','approved','rejected','returned','rerouted','closed');
--;;
CREATE TYPE referee_status AS ENUM ('created','recommended','rejected','returned');
--;;
CREATE TYPE license_status AS ENUM ('approved','rejected');
--;;
CREATE TYPE license_state AS ENUM ('created','approved','rejected');
--;;
CREATE TYPE attachment_state AS ENUM ('visible','hidden');
--;;
CREATE TYPE reviewers_state AS ENUM ('created','commented');
--;;
CREATE TYPE application_state AS ENUM ('applied','approved','rejected','returned','closed','draft','onhold');
--;;
CREATE TYPE item_state AS ENUM ('disabled','enabled','copied');
--;;
CREATE TYPE prefix_state AS ENUM ('applied','approved','denied');
--;;
CREATE TYPE license_type AS ENUM ('text','attachment','link');
--;;
CREATE TABLE rms_resource_prefix (
  id serial NOT NULL PRIMARY KEY,
  modifierUserId bigint NOT NULL,
  prefix varchar(255) DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_resource (
  id serial NOT NULL PRIMARY KEY,
  modifierUserId bigint NOT NULL,
  rsPrId integer DEFAULT NULL,
  prefix varchar(255) NOT NULL,
  resId varchar(255) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_workflow (
  id serial NOT NULL PRIMARY KEY,
  ownerUserId bigint NOT NULL,
  modifierUserId bigint NOT NULL,
  title varchar(256) NOT NULL,
  fnlround integer NOT NULL,
  visibility scope NOT NULL DEFAULT 'private',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_application_form_meta (
  id serial NOT NULL PRIMARY KEY,
  ownerUserId bigint NOT NULL,
  modifierUserId bigint NOT NULL,
  title varchar(256) DEFAULT NULL,
  visibility scope NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_catalogue_item (
  id serial NOT NULL PRIMARY KEY,
  title varchar(256) NOT NULL,
  resId integer DEFAULT NULL,
  wfId integer DEFAULT NULL,
  formId integer DEFAULT '1',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_ibfk_1 FOREIGN KEY (resId) REFERENCES rms_resource (id),
  CONSTRAINT rms_catalogue_item_ibfk_2 FOREIGN KEY (wfId) REFERENCES rms_workflow (id),
  CONSTRAINT rms_catalogue_item_ibfk_3 FOREIGN KEY (formId) REFERENCES rms_application_form_meta (id)
);
--;;
CREATE TABLE rms_catalogue_item_application (
  id serial NOT NULL PRIMARY KEY,
  catId integer DEFAULT NULL,
  applicantUserId bigint NOT NULL,
  fnlround integer NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  modifierUserId bigint DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_ibfk_1 FOREIGN KEY (catId) REFERENCES rms_catalogue_item (id)
);
--;;
CREATE TABLE rms_application_form (
  id serial NOT NULL PRIMARY KEY,
  ownerUserId bigint NOT NULL,
  modifierUserId bigint NOT NULL,
  title varchar(256) NOT NULL,
  visibility scope NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_application_form_item (
  id serial NOT NULL PRIMARY KEY,
  ownerUserId bigint NOT NULL,
  modifierUserId bigint NOT NULL,
  title varchar(256) NOT NULL,
  toolTip varchar(256) DEFAULT NULL,
  inputPrompt varchar(256) DEFAULT NULL,
  type itemtype DEFAULT NULL,
  value bigint NOT NULL,
  visibility scope NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_application_form_item_map (
  id serial NOT NULL PRIMARY KEY,
  formId integer DEFAULT NULL,
  formItemId integer DEFAULT NULL,
  formItemOptional bit(1) DEFAULT b'0',
  modifierUserId bigint NOT NULL,
  itemOrder integer DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_application_form_item_map_ibfk_1 FOREIGN KEY (formId) REFERENCES rms_application_form (id),
  CONSTRAINT rms_application_form_item_map_ibfk_2 FOREIGN KEY (formItemId) REFERENCES rms_application_form_item (id)
);
--;;
CREATE TABLE rms_attachment (
  id serial NOT NULL PRIMARY KEY,
  userId bigint NOT NULL,
  title varchar(256) NOT NULL,
  file_name varchar(256) NOT NULL,
  file_type varchar(15) DEFAULT NULL,
  file_size bigint NOT NULL,
  file_content bytea NOT NULL,
  file_ext varchar(10) NOT NULL,
  state attachment_state NOT NULL DEFAULT 'visible',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_license (
  id serial NOT NULL PRIMARY KEY,
  ownerUserId bigint NOT NULL,
  modifierUserId bigint NOT NULL,
  title varchar(256) NOT NULL,
  type license_type NOT NULL,
  textContent varchar(16384) DEFAULT NULL,
  attId integer DEFAULT NULL,
  visibility scope NOT NULL DEFAULT 'private',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_license_ibfk_1 FOREIGN KEY (attId) REFERENCES rms_attachment (id)
);
--;;
CREATE TABLE rms_application_attachment_values (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  formMapId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  attachmentId integer DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_application_attachment_values_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_application_attachment_values_ibfk_2 FOREIGN KEY (formMapId) REFERENCES rms_application_form_item_map (id),
  CONSTRAINT rms_application_attachment_values_ibfk_3 FOREIGN KEY (attachmentId) REFERENCES rms_attachment (id)
);
--;;
CREATE TABLE rms_application_checkbox_values (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  formMapId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  value bit(1) DEFAULT b'0',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_application_checkbox_values_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_application_checkbox_values_ibfk_2 FOREIGN KEY (formMapId) REFERENCES rms_application_form_item_map (id)
);
--;;
CREATE TABLE rms_application_form_item_string_values (
  id serial NOT NULL PRIMARY KEY,
  formItemId integer DEFAULT NULL,
  value varchar(4096) NOT NULL,
  itemOrder integer DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_application_form_item_string_values_ibfk_1 FOREIGN KEY (formItemId) REFERENCES rms_application_form_item (id)
);
--;;
CREATE TABLE rms_application_form_meta_map (
  id serial NOT NULL PRIMARY KEY,
  metaFormId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  langCode varchar(64) DEFAULT NULL,
  formId integer DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_application_form_meta_map_ibfk_1 FOREIGN KEY (metaFormId) REFERENCES rms_application_form_meta (id),
  CONSTRAINT rms_application_form_meta_map_ibfk_2 FOREIGN KEY (formId) REFERENCES rms_application_form (id)
);
--;;
CREATE TABLE rms_application_license_approval_values (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  formMapId integer DEFAULT NULL,
  licId integer NOT NULL,
  modifierUserId bigint DEFAULT NULL,
  state license_status NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_application_license_approval_values_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_application_license_approval_values_ibfk_2 FOREIGN KEY (formMapId) REFERENCES rms_application_form_item_map (id),
  CONSTRAINT rms_application_license_approval_values_ibfk_3 FOREIGN KEY (licId) REFERENCES rms_license (id)
);
--;;
CREATE TABLE rms_application_referee_invite_values (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  formMapId integer DEFAULT NULL,
  modifierUserId bigint DEFAULT NULL,
  email varchar(256) NOT NULL,
  hash varchar(256) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_application_referee_invite_values_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_application_referee_invite_values_ibfk_2 FOREIGN KEY (formMapId) REFERENCES rms_application_form_item_map (id)
);
--;;
CREATE TABLE rms_application_referee_values (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  formMapId integer DEFAULT NULL,
  refereeUserId bigint NOT NULL,
  comment varchar(4096) DEFAULT NULL,
  state referee_status NOT NULL DEFAULT 'created',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_application_referee_values_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_application_referee_values_ibfk_2 FOREIGN KEY (formMapId) REFERENCES rms_application_form_item_map (id)
);
--;;
CREATE TABLE rms_application_text_values (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  formMapId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  value varchar(4096) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_application_text_values_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_application_text_values_ibfk_2 FOREIGN KEY (formMapId) REFERENCES rms_application_form_item_map (id)
);
--;;
CREATE TABLE rms_workflow_approvers (
  id serial NOT NULL PRIMARY KEY,
  wfId integer DEFAULT NULL,
  apprUserId bigint NOT NULL,
  round integer NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_workflow_approvers_ibfk_1 FOREIGN KEY (wfId) REFERENCES rms_workflow (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_approvers (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  wfApprId integer DEFAULT NULL,
  apprUserId bigint NOT NULL,
  round integer NOT NULL,
  comment varchar(4096) DEFAULT NULL,
  state approval_status DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_approvers_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_catalogue_item_application_approvers_ibfk_2 FOREIGN KEY (wfApprId) REFERENCES rms_workflow_approvers (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_catid_overflow (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  catId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_catid_overflow_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_catalogue_item_application_catid_overflow_ibfk_2 FOREIGN KEY (catId) REFERENCES rms_catalogue_item (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_free_comment_values (
  id serial NOT NULL PRIMARY KEY,
  userId bigint NOT NULL,
  catAppId integer DEFAULT NULL,
  comment varchar(4096) DEFAULT NULL,
  public bit(1) NOT NULL DEFAULT b'1',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_free_comment_values_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_licenses (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  licId integer DEFAULT NULL,
  actorUserId bigint NOT NULL,
  round integer NOT NULL,
  stalling bit(1) NOT NULL DEFAULT b'0',
  state license_state NOT NULL DEFAULT 'created',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_licenses_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_catalogue_item_application_licenses_ibfk_2 FOREIGN KEY (licId) REFERENCES rms_license (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_member_invite_values (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  modifierUserId bigint DEFAULT NULL,
  email varchar(256) NOT NULL,
  hash varchar(256) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_member_invite_values_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_members (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  memberUserId bigint NOT NULL,
  modifierUserId bigint DEFAULT '-1',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_members_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_metadata (
  id serial NOT NULL PRIMARY KEY,
  userId bigint NOT NULL,
  catAppId integer DEFAULT NULL,
  key varchar(32) NOT NULL,
  value varchar(256) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_metadata_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_predecessor (
  id serial NOT NULL PRIMARY KEY,
  pre_catAppId integer DEFAULT NULL,
  suc_catAppId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_predecessor_ibfk_1 FOREIGN KEY (pre_catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_catalogue_item_application_predecessor_ibfk_2 FOREIGN KEY (suc_catAppId) REFERENCES rms_catalogue_item_application (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_publications (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  publication varchar(512) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_publications_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_reviewers (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  revUserId bigint NOT NULL,
  modifierUserId bigint DEFAULT NULL,
  round integer NOT NULL,
  comment varchar(4096) DEFAULT NULL,
  state reviewers_state NOT NULL DEFAULT 'created',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_reviewers_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_state (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  curround integer NOT NULL,
  state application_state DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_state_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id)
);
--;;
CREATE TABLE rms_catalogue_item_application_state_reason (
  id serial NOT NULL PRIMARY KEY,
  catAppId integer NOT NULL,
  catAppStateId integer NOT NULL,
  modifierUserId bigint NOT NULL,
  reason varchar(32) NOT NULL,
  comment varchar(4096) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_application_state_reason_ibfk_1 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id),
  CONSTRAINT rms_catalogue_item_application_state_reason_ibfk_2 FOREIGN KEY (catAppStateId) REFERENCES rms_catalogue_item_application_state (id)
);
--;;
CREATE TABLE rms_catalogue_item_localization (
  id serial NOT NULL PRIMARY KEY,
  catId integer DEFAULT NULL,
  langCode varchar(64) NOT NULL,
  title varchar(256) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_localization_ibfk_1 FOREIGN KEY (catId) REFERENCES rms_catalogue_item (id)
);
--;;
CREATE TABLE rms_catalogue_item_predecessor (
  id serial NOT NULL PRIMARY KEY,
  pre_catId integer DEFAULT NULL,
  suc_catId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_predecessor_ibfk_1 FOREIGN KEY (pre_catId) REFERENCES rms_catalogue_item (id),
  CONSTRAINT rms_catalogue_item_predecessor_ibfk_2 FOREIGN KEY (suc_catId) REFERENCES rms_catalogue_item (id)
);
--;;
CREATE TABLE rms_catalogue_item_state (
  id serial NOT NULL PRIMARY KEY,
  catId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  state item_state DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_catalogue_item_state_ibfk_1 FOREIGN KEY (catId) REFERENCES rms_catalogue_item (id)
);
--;;
CREATE TABLE rms_entitlement (
  id serial NOT NULL PRIMARY KEY,
  resId integer DEFAULT NULL,
  catAppId integer DEFAULT NULL,
  userId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_entitlement_ibfk_1 FOREIGN KEY (resId) REFERENCES rms_resource (id),
  CONSTRAINT rms_entitlement_ibfk_2 FOREIGN KEY (catAppId) REFERENCES rms_catalogue_item_application (id)
);
--;;
CREATE TABLE rms_entitlement_ebi (
  id serial NOT NULL PRIMARY KEY,
  eppn varchar(255) NOT NULL,
  domain varchar(255) NOT NULL,
  resource varchar(255) NOT NULL,
  dacId varchar(256) NOT NULL,
  userId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_entitlement_saml (
  id serial NOT NULL PRIMARY KEY,
  eppn varchar(255) NOT NULL,
  domain varchar(255) NOT NULL,
  resource varchar(255) NOT NULL,
  entityId varchar(256) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_entitlement_saml_migration (
  id serial NOT NULL PRIMARY KEY,
  eppn varchar(255) NOT NULL,
  domain varchar(255) NOT NULL,
  resource varchar(255) NOT NULL,
  entityId varchar(256) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_invitations (
  id serial NOT NULL PRIMARY KEY,
  email varchar(256) NOT NULL,
  hash varchar(256) NOT NULL,
  userId bigint DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_license_localization (
  id serial NOT NULL PRIMARY KEY,
  licId integer DEFAULT NULL,
  langCode varchar(64) NOT NULL,
  title varchar(256) NOT NULL,
  textContent varchar(16384) DEFAULT NULL,
  attId integer DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_license_localization_ibfk_1 FOREIGN KEY (licId) REFERENCES rms_license (id),
  CONSTRAINT rms_license_localization_ibfk_2 FOREIGN KEY (attId) REFERENCES rms_attachment (id)
);
--;;
CREATE TABLE rms_license_references (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  licId integer DEFAULT NULL,
  referenceName varchar(64) NOT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_license_references_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id),
  CONSTRAINT rms_license_references_ibfk_2 FOREIGN KEY (licId) REFERENCES rms_license (id)
);
--;;
CREATE TABLE rms_manifestations (
  id serial NOT NULL PRIMARY KEY,
  manifId varchar(256) NOT NULL,
  resId integer NOT NULL,
  manifConf varchar(256) NOT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_manifestations_ibfk_1 FOREIGN KEY (resId) REFERENCES rms_resource (id)
);
--;;
CREATE TABLE rms_resource_close_period (
  id serial NOT NULL PRIMARY KEY,
  resId integer DEFAULT NULL,
  closePeriod integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_close_period_ibfk_1 FOREIGN KEY (resId) REFERENCES rms_resource (id)
);
--;;
CREATE TABLE rms_resource_licenses (
  id serial NOT NULL PRIMARY KEY,
  resId integer DEFAULT NULL,
  licId integer DEFAULT NULL,
  stalling bit(1) NOT NULL DEFAULT b'0',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_licenses_ibfk_1 FOREIGN KEY (resId) REFERENCES rms_resource (id),
  CONSTRAINT rms_resource_licenses_ibfk_2 FOREIGN KEY (licId) REFERENCES rms_license (id)
);
--;;
CREATE TABLE rms_resource_link_location (
  id serial NOT NULL PRIMARY KEY,
  resId integer DEFAULT NULL,
  link varchar(2048) NOT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_link_location_ibfk_1 FOREIGN KEY (resId) REFERENCES rms_resource (id)
);
--;;
CREATE TABLE rms_resource_mf_ebi_dac_target (
  id serial NOT NULL PRIMARY KEY,
  resId integer DEFAULT NULL,
  dacId varchar(256) NOT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_mf_ebi_dac_target_ibfk_1 FOREIGN KEY (resId) REFERENCES rms_resource (id)
);
--;;
CREATE TABLE rms_resource_mf_saml_target (
  id serial NOT NULL PRIMARY KEY,
  resId integer DEFAULT NULL,
  entityId varchar(256) NOT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_mf_saml_target_ibfk_1 FOREIGN KEY (resId) REFERENCES rms_resource (id)
);
--;;
CREATE TABLE rms_resource_prefix_allow_form_editing (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  enabled bit(1) DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_allow_form_editing_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_allow_members (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  enabled bit(1) DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_allow_members_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_allow_updates (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  enabled bit(1) DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_allow_updates_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_application (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  application varchar(2048) DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_application_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_certificates (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  subjectDn varchar(256) DEFAULT NULL,
  base64content varchar(16384) DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_certificates_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_close_period (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  closePeriod integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_close_period_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_default_form (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  metaFormId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_default_form_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id),
  CONSTRAINT rms_resource_prefix_default_form_ibfk_2 FOREIGN KEY (metaFormId) REFERENCES rms_application_form_meta (id)
);
--;;
CREATE TABLE rms_resource_prefix_link_location (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  link varchar(2048) NOT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_link_location_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_mf_ebi (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  enabled bit(1) DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_mf_ebi_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_owners (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  ownerUserId bigint NOT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_owners_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_refresh_period (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  refreshPeriod integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_refresh_period_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_reporters (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  reporterUserId bigint NOT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_reporters_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_prefix_state (
  id serial NOT NULL PRIMARY KEY,
  rsPrId integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  state prefix_state NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_prefix_state_ibfk_1 FOREIGN KEY (rsPrId) REFERENCES rms_resource_prefix (id)
);
--;;
CREATE TABLE rms_resource_refresh_period (
  id serial NOT NULL PRIMARY KEY,
  resId integer DEFAULT NULL,
  refreshPeriod integer DEFAULT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_refresh_period_ibfk_1 FOREIGN KEY (resId) REFERENCES rms_resource (id)
);
--;;
CREATE TABLE rms_resource_state (
  id serial NOT NULL PRIMARY KEY,
  resId integer DEFAULT NULL,
  ownerUserId bigint NOT NULL,
  modifierUserId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_resource_state_ibfk_1 FOREIGN KEY (resId) REFERENCES rms_resource (id)
);
--;;
CREATE TABLE rms_user_selection_names (
  id serial NOT NULL PRIMARY KEY,
  actionId bigint NOT NULL,
  groupId integer NOT NULL,
  listName varchar(32) DEFAULT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_user_selections (
  id serial NOT NULL PRIMARY KEY,
  actionId bigint NOT NULL,
  groupId integer NOT NULL,
  userId bigint NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL
);
--;;
CREATE TABLE rms_workflow_approver_options (
  id serial NOT NULL PRIMARY KEY,
  wfApprId integer DEFAULT NULL,
  keyValue varchar(256) NOT NULL,
  optionValue varchar(256) NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_workflow_approver_options_ibfk_1 FOREIGN KEY (wfApprId) REFERENCES rms_workflow_approvers (id)
);
--;;
CREATE TABLE rms_workflow_licenses (
  id serial NOT NULL PRIMARY KEY,
  wfId integer DEFAULT NULL,
  licId integer DEFAULT NULL,
  round integer NOT NULL,
  stalling bit(1) NOT NULL DEFAULT b'0',
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_workflow_licenses_ibfk_1 FOREIGN KEY (wfId) REFERENCES rms_workflow (id),
  CONSTRAINT rms_workflow_licenses_ibfk_2 FOREIGN KEY (licId) REFERENCES rms_license (id)
);
--;;
CREATE TABLE rms_workflow_reviewers (
  id serial NOT NULL PRIMARY KEY,
  wfId integer DEFAULT NULL,
  revUserId bigint NOT NULL,
  round integer NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_workflow_reviewers_ibfk_1 FOREIGN KEY (wfId) REFERENCES rms_workflow (id)
);
--;;
CREATE TABLE rms_workflow_round_min (
  id serial NOT NULL PRIMARY KEY,
  wfId integer DEFAULT NULL,
  min integer NOT NULL,
  round integer NOT NULL,
  start timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  endt timestamp NULL DEFAULT NULL,
  CONSTRAINT rms_workflow_round_min_ibfk_1 FOREIGN KEY (wfId) REFERENCES rms_workflow (id)
);