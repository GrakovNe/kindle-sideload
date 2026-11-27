-- Both shelf_item lookups used by the web shelf were full scans: the shelf page selects the active
-- items of one shelf, and the TTL termination handler selects an item by its environment id.
CREATE INDEX idx_shelf_item_shelf_id_status ON shelf_item (shelf_id, status);
CREATE INDEX idx_shelf_item_environment_id ON shelf_item (environment_id);
