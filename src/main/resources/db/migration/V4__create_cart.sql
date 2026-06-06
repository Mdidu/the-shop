CREATE TABLE carts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE cart_items (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id     UUID           NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id  UUID           NOT NULL REFERENCES products(id),
    quantity    INTEGER        NOT NULL,
    unit_price  NUMERIC(10, 2) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT cart_items_unique_product UNIQUE (cart_id, product_id),
    CONSTRAINT cart_items_quantity_check CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
