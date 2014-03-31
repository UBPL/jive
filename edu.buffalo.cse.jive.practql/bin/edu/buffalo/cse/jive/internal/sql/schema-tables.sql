/** 
 * ************************************************************
 *  NON-TEMPORAL: VALUE MODEL SECTION                    
 * ************************************************************
 */

CREATE TABLE value (
  vid bigint,                 -- value identifier
  kind ValueKind,             -- kind of this value
  value text                  -- textual representation of this value
);
COMMENT ON TABLE value IS 'Mapping from IValue. All values have a kind and a textual representation.';

CREATE TABLE value_cref (
  vid bigint,                 -- references value.vid
  kind ValueKind,             -- kind of this value
  contourId bigint            -- references contour_context.contourId
);
COMMENT ON TABLE value_cref IS 'Mapping from IContourReference and IMethodContourReference. All in-model contour references.';

CREATE TABLE value_om_mcref (
  vid bigint,                 -- references value.vid
  methodId bigint             -- references contour_method.contourId
);
COMMENT ON TABLE value_om_mcref IS 'Mapping from IOutOfModelMethodReference. All out-of-model method references associated with an eventual in-model method contour.';

CREATE TABLE value_om_mkeyref (
  vid bigint,                 -- references value.vid
  methodKey text              -- out-of-model method signature
);
COMMENT ON TABLE value_om_mkeyref IS 'Mapping from IOutOfModelMethodKeyReference. All out-of-model method references associated with an out-of-model method described a key (e.g., bridge method).';

CREATE TABLE value_om_resolved (
  vid bigint,                 -- references value.vid
  typeName text               -- actual run-time type name of the resolved value
);
COMMENT ON TABLE value_om_resolved IS 'Mapping from IResolvedValue. All resolved value references (e.g., Comparable resolved as Integer).';

CREATE TABLE value_thread (
  vtid bigint,                -- thread identifier
  name text                   -- thread name
);
COMMENT ON TABLE value_thread IS 'Mapping from IThreadValue. All run-time thread values.';

CREATE TABLE value_file (
  vfid bigint,                -- file identifier
  name text                   -- file name
);
COMMENT ON TABLE value_file IS 'Mapping from IFileValue. All source code files executed.';

CREATE TABLE value_line (
  vlid bigint,                -- line identifier
  vfid bigint,                -- references value_file.vfid
  number int                  -- line number
);
COMMENT ON TABLE value_line IS 'Mapping from ILineValue. All source code lines executed.';

/** 
 * ************************************************************
 *  NON-TEMPORAL: NODE MODEL SECTION                     
 * ************************************************************
 */

CREATE TABLE node (
  nid bigint,                 -- unique node identifier
  kind NodeKind,
  lineFrom int,
  lineTo int,
  modifiers NodeModifier[],
  name text,
  origin NodeOrigin,
  parentId bigint,            -- references node.nid
  visibility NodeVisibility
);
COMMENT ON TABLE node IS 'Mapping from ÌNode. Nodes represent the subject application''s static model.';

CREATE TABLE node_type (
  nid bigint,                 -- references node.nid
  key text,                   -- type key
  containerId bigint,         -- references node.nid of a root or file node
  defaultValueId int,         -- references value.vid
  superClassRefId text        -- references noderef_type.refId
);
COMMENT ON TABLE node_type IS 'Mapping from ITypeNode. Describes a type declared within a source file. Types may declare data members, method members, and other types. The node''s parent determines the the''s declaring type, if not a top-level type, or file, in the case of a top-level type.';

CREATE TABLE node_data (
  nid bigint,                 -- references node.nid
  index int,                  -- index of this member in its parent declaration
  typeRefId bigint            -- references noderef_type.refId
);
COMMENT ON TABLE node_data IS 'Mapping from IDataNode. Describes a field or local variable declared within a type or method, respectively. The node''s parent determines the data node''s declaring environment.';

CREATE TABLE node_method (
  nid bigint,                 -- references node.nid
  key text,                   -- method key
  index int,                  -- index of this member in its parent declaration
  returnTypeRefId bigint      -- references noderef_type.refId
);
COMMENT ON TABLE node_method IS 'Mapping from IMethodNode. Describes a method declared within a type. The node''s parent determines the method''s declaring type.';

CREATE TABLE noderef_method (
  refId bigint,               -- unique reference identifier
  key text                    -- node key used to resolve the node at run-time
);
COMMENT ON TABLE noderef_method IS 'Mapping from ÌMethodNodeRef. References to methods that *may* not have been resolved statically.';

CREATE TABLE noderef_type (
  refId bigint,               -- unique reference identifier
  key text                    -- node key used to resolve the node at run-time
);
COMMENT ON TABLE noderef_type IS 'Mapping from ÌTypeNodeRef. References to types that *may* not have been resolved statically.';

CREATE TABLE node_type_interface (
  nid bigint,                 -- references node_type.nid
  refId bigint                -- references noderef_type.refId 
);
COMMENT ON TABLE node_type_interface IS 'Mapping of the relationship type (1) --> (0..N) interface, which defines the ''super interfaces'' of the type.';

CREATE TABLE node_method_exception (
  nid bigint,                 -- references node_method.nid
  refId bigint                -- references noderef_type.refId 
);
COMMENT ON TABLE node_method_exception IS 'Mapping of the relationship method (1) --> (0..N) thrown exception.';

/** 
 * ************************************************************
 *  NON-TEMPORAL: DEPENDENCE GRAPH MODEL SECTION               
 * ************************************************************
 */
CREATE TABLE rnode (
  rnid bigint,                -- unique resolved node identifier
  isActual boolean,           -- whether this node appears in an argument position
  isLHS boolean,              -- whether this node appears on the left hand side of an assignment
  qualifierOf bigint,         -- if this node qualifies another node, the qualifier references rnode.rnid
  sourceIndex int             -- the source index of this node (within its respective method)
);
COMMENT ON TABLE rnode IS 'Mapping from IResolvedNode. A node in the method dependence graph representing a method call, data, lazy data, or this reference.';

CREATE TABLE rcall (
  rnid bigint,                -- references rnode.rnid
  methodRefId bigint,         -- references noderef_method.refId 
  size int                    -- number of argument positions in the call
);
COMMENT ON TABLE rcall IS 'Mapping from IResolvedCall. A node in the method dependence graph representing a method call.';

CREATE TABLE rdata (
  rnid bigint,                -- references rnode.rnid
  dataId bigint,              -- references node_data.nid
  isDef boolean               -- determines if this is a defined node
);
COMMENT ON TABLE rdata IS 'Mapping from IResolvedData. A node in the method dependence graph representing a field or variable reference.';

CREATE TABLE rdata_lazy (
  rnid bigint,                -- references rnode.rnid
  name text,                  -- name of this data memeber
  typeRefId bigint,           -- references noderef_type.refId
  isDef boolean               -- determines if this is a defined node
);
COMMENT ON TABLE rdata_lazy IS 'Mapping from IResolvedLazyData. A node in the method dependence graph representing a lazily resolved field reference.';

CREATE TABLE rthis (
  rnid bigint,                -- references rnode.rnid
  typeId bigint               -- references node_type.nid
);
COMMENT ON TABLE rthis IS 'Mapping from IResolvedThis. A node in the method dependence graph representing a reference to a current type instance.';

CREATE TABLE mdg (
  methodId bigint,            -- references the method node for which the mdg is stored
  line int,                   -- this mdg's entry point line number
  hasSystemExit boolean       -- whether this method has a system exit (locally or transitively)
);
COMMENT ON TABLE mdg IS 'Mapping from IMethodDependenceGraph. A method dependence graph for the given method node.';

CREATE TABLE rline (
  methodId bigint,            -- references the method node containing this line
  line int,                   -- this line's number
  parentLine int,             -- this line's parent line (must be in the same method)
  hasConditional boolean,
  isControl boolean,
  isLoopControl boolean,
  kind LineKind
);
COMMENT ON TABLE rline IS 'Mapping from IResolvedLine. A node in the method dependence graph representing a line which, by assumption, contains a single statement.';

CREATE TABLE rcall_uses (
  rcallId bigint,             -- references rcall.rnid
  argument int,               -- argument position in which this use appears
  rnid bigint                 -- references rnode.rnid
);
COMMENT ON TABLE rcall_uses IS 'Mapping of the relationship resolved call (1) --> (0..N) uses. Any number of nested calls and data references may appear in an argument position of a method call.';

CREATE TABLE rline_defs (
  methodId bigint,            -- references the method node containing this line
  line int,                   -- references the line number for which a def is recorded
  rnid bigint                 -- references rnode.rnid
);
COMMENT ON TABLE rline_defs IS 'Mapping of the relationship resolved line (1) --> (0..N) data definition. Any number of field/local variables modified in the source line.';

CREATE TABLE rline_jumps (
  methodId bigint,            -- references the method node containing this line
  line int,                   -- references the line number for which a jump is recorded
  jumpLine int                -- jump line
);
COMMENT ON TABLE rline_jumps IS 'Mapping of the relationship resolved line (1) --> (0..N) jump. Any number non-structured jumps on which this source line is control dependent.';

CREATE TABLE rline_uses (
  methodId bigint,            -- references the method node containing this line
  line int,                   -- references the line number for which a use is recorded
  rnid bigint                 -- references rnode.rnid
);
COMMENT ON TABLE rline_uses IS 'Mapping of the relationship resolved line (1) --> (0..N) uses. Any number method calls and data uses appearing in this line.';

/** 
 * ************************************************************
 *  TEMPORAL: CONTOUR MODEL SECTION                   
 * ************************************************************
 */

/** 
 * The join with fromTime determines the type of contour:
 *
 *   METHOD_CALL...: Method Contour
 *   NEW_OBJECT....: Instance Contour
 *   TYPE_LOAD.....: Static Contour
 *
 * In the case of a method contour, the join with event also 
 * determines the thread under which the method executes. If
 * this is a static or instance contour, schemaId references 
 * node_type.nid, otherwise it references node_method.nid.
 */
CREATE UNLOGGED TABLE contour (
  contourId bigint,           -- unique contour identifier
  schemaId bigint,            -- references node.nid (ITypeNode.nid or IMethodNode.nid)
  parentId bigint,            -- references contour.contourId
  ordinal int,                -- ordinal value associated with the contour (weak identifier for contours with the same schemaId/parentId pair)
  kind ContourKind,           -- kind of this contour
  fromTime bigint,            -- references event.eventId (IMethodCall, ITypeLoadEvent, or INewObject)
  toTime bigint               -- references event.eventId (IMethodExit, ITypeUnloadEvent, or IDeleteObject)
);
COMMENT ON TABLE contour IS 'Mapping from IContour. A contour represents a class, object, or method environment. It is a temporal entity that has exists for a well determined time interval.';

CREATE UNLOGGED TABLE contour_member (
  contourId bigint,           -- references contour.contourId
  schemaId bigint,            -- references node_data.nid
  valueId bigint,             -- references value.vid
  fromTime bigint,            -- references event.eventId (IFieldAssignEvent or IVarAssignEvent)
  toTime bigint               -- references event.eventId (IFieldAssignEvent, IVarAssignEvent, or IVarDeleteEvent)
);
COMMENT ON TABLE contour_member IS 'Mapping from IContourMember. A contour member represents a field or local variable.';

CREATE OR REPLACE FUNCTION tg_contour_after_insert() RETURNS trigger AS $$
BEGIN
  IF (NEW.kind = 'CK_METHOD'::ContourKind) THEN
    INSERT INTO contour_member(contourId, schemaId, valueId, fromTime)
    SELECT 
      NEW.contourId,
      nd.nId,
      CASE WHEN 'NM_RPDL'::NodeModifier = ANY (COALESCE(nd.modifiers, '{}'::NodeModifier[])) THEN e.callerId
      ELSE v.vId END,
      NEW.fromTime
    FROM
      (node NATURAL JOIN node_data) nd 
      INNER JOIN event_mcall e ON (e.eventId = NEW.fromTime),
      value v
    WHERE
      nd.parentId = NEW.schemaId AND v.kind = 'UNINITIALIZED'::ValueKind;
  ELSE
    INSERT INTO contour_member(contourId, schemaId, valueId, fromTime)
    SELECT 
      NEW.contourId,
      nd.nId,
      ndt.defaultValueId,
      NEW.fromTime
    FROM
      (node NATURAL JOIN node_data) nd 
      LEFT JOIN (node NATURAL JOIN node_type) ndt ON (ndt.nId = nd.typeRefId)
    WHERE
      nd.parentId = NEW.schemaId;
  END IF;
  RETURN NEW;
END; $$ LANGUAGE 'plpgsql';
COMMENT ON FUNCTION tg_contour_after_insert IS 'A contour creation triggers the creation of the respective contour members with their default values.';

CREATE OR REPLACE FUNCTION tg_cmember_after_insert() RETURNS trigger AS $$
BEGIN
  UPDATE contour_member SET toTime = NEW.fromTime
  WHERE contourId = NEW.contourId AND schemaId = NEW.schemaId AND fromTime < NEW.fromTime AND toTime IS NULL;
  RETURN NEW;
END; $$ LANGUAGE 'plpgsql';
COMMENT ON FUNCTION tg_cmember_after_insert IS 'The insert of a contour member always happens with an empty toTime. The trigger updates the toTime of the most recent value of the contour member, if its toTime is empty.';

CREATE TRIGGER tg_contour_after_insert 
  AFTER INSERT ON contour 
  FOR EACH ROW 
  EXECUTE PROCEDURE tg_contour_after_insert();

CREATE TRIGGER tg_cmember_after_insert 
  AFTER INSERT ON contour_member
  FOR EACH ROW 
  EXECUTE PROCEDURE tg_cmember_after_insert();

/** 
 * ************************************************************
 *  TEMPORAL: EVENT MODEL SECTION                    
 * ************************************************************
 */

CREATE UNLOGGED TABLE event (
  eventId bigint,             -- unique event time
  parentId bigint,            -- references event.eventId (kind is method call, thread start, or system start)
  threadId bigint,            -- references value_thread.vid
  lineId bigint,              -- references node_line.lid
  kind EventKind
);
COMMENT ON TABLE event IS 'Mapping from DataEvent. A run-time trace event.';

CREATE UNLOGGED TABLE event_ecatch (
  eventId bigint,             -- references event.eventId
  exceptionId bigint          -- references value.vid
);
COMMENT ON TABLE event_ecatch IS 'Mapping from ExceptionCatchEvent. Identifies the caught exception.';

CREATE UNLOGGED TABLE event_ethrow (
  eventId bigint,             -- references event.eventId
  throwerId bigint,           -- references value.vid (if out-of-model) or value_cref (if in-model)
  exceptionId bigint,         -- references value.vid
  framePopped boolean
);
COMMENT ON TABLE event_ethrow IS 'Mapping from ExceptionThrowEvent. Identifies the thrown exception, the context in which it was thrown, and whether a frame was popped.';

CREATE UNLOGGED TABLE event_mcall (
  eventId bigint,             -- references event.eventId
  callerId bigint,            -- references value.vid
  targetId bigint             -- references value.vid
);
COMMENT ON TABLE event_mcall IS 'Mapping from MethodCallEvent. Identifies a call made from the given caller which initiate the execution of the given target.';

CREATE UNLOGGED TABLE event_mexit (
  eventId bigint,             -- references event.eventId
  returnContextId bigint,     -- references value.vid (if out-of-model) or value_cref (if in-model)
  returnValueId bigint        -- references value.vid
);
COMMENT ON TABLE event_mexit IS 'Mapping from MethodExitEvent. Identifies the returned value from a method terminating its execution.';

CREATE UNLOGGED TABLE event_fread (
  eventId bigint,             -- references event.eventId
  contourId bigint,           -- references contour_member.contourId
  schemaId bigint             -- references contour_member.schemaId
);
COMMENT ON TABLE event_fread IS 'Mapping from FieldReadEvent. Identifies the contour member that was read.';

-- TODO check if this can be pushed to a view (based on the most recently completed call on this thread)
CREATE UNLOGGED TABLE event_mret (
  eventId bigint,             -- references event.eventId
  terminatorId bigint         -- references event.eventId
);
COMMENT ON TABLE event_mret IS 'Mapping from MethodReturnedEvent. Identifies the terminator event associated with this returned event.';

CREATE OR REPLACE FUNCTION tg_ethrow_after_insert() RETURNS trigger AS $$
BEGIN
  IF (NEW.framePopped) THEN
    UPDATE contour SET toTime = NEW.eventId
    WHERE contourId = (SELECT contourId FROM value_cref WHERE vid = NEW.throwerId);
    
    UPDATE contour_member SET toTime = NEW.eventId
    WHERE toTime IS NULL AND contourId = (SELECT contourId FROM value_cref WHERE vid = NEW.throwerId);
  END IF;
  RETURN NEW;
END; $$ LANGUAGE 'plpgsql';
COMMENT ON FUNCTION tg_ethrow_after_insert IS 'A throw event that pops a frame removes a method contour from the execution. The toTime of the respective contour and its members must be updated.';

CREATE OR REPLACE FUNCTION tg_mexit_after_insert() RETURNS trigger AS $$
BEGIN
  UPDATE contour c SET toTime = NEW.eventId
  FROM value_cref cr
  WHERE c.contourId = cr.contourId AND cr.vid = NEW.returnContextId;

  UPDATE contour_member c SET toTime = NEW.eventId
  FROM value_cref cr
  WHERE c.contourId = cr.contourId AND cr.vid = NEW.returnContextId;
  RETURN NEW;
END; $$ LANGUAGE 'plpgsql';
COMMENT ON FUNCTION tg_mthrow_after_insert IS 'A method exit pops a frame removes a method contour from the execution. The toTime of the respective contour and its members must be updated.';

CREATE TRIGGER tg_ethrow_after_insert 
  AFTER INSERT ON event_ethrow
  FOR EACH ROW 
  EXECUTE PROCEDURE tg_ethrow_after_insert();
  
CREATE TRIGGER tg_mexit_after_insert 
  AFTER INSERT ON event_mexit
  FOR EACH ROW 
  EXECUTE PROCEDURE tg_mexit_after_insert();

