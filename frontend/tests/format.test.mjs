import { test } from 'node:test';
import assert from 'node:assert/strict';
import { grams, orderPreview, percentToFraction, availableStock, lowStockProducts } from '../src/format.ts';

test('order preview applies fractional rates and rounds once after the final calculation', () => {
  assert.equal(orderPreview([{ price: '50.00', quantity: 2 }], '10', '19'), '107,10 €');
  assert.equal(orderPreview([{ price: '33.33', quantity: 3 }], '10', '19'), '107,09 €');
  assert.equal(orderPreview([{ price: '0.10', quantity: 1 }, { price: '0.2', quantity: 1 }], '0', '0'), '0,30 €');
  assert.equal(orderPreview([{ price: '0.005', quantity: 1 }], '0', '0'), '0,01 €');
  assert.equal(orderPreview([{ price: '50', quantity: 2 }], '100', '19'), '0,00 €');
  assert.equal(orderPreview([{ price: '50', quantity: 0 }], '0', '0'), null);
});

test('percent input is converted to exact decimal strings and weight uses grams', () => {
  assert.equal(percentToFraction('19'), '0.19');
  assert.equal(percentToFraction('2.5'), '0.025');
  assert.equal(percentToFraction('100'), '1.00');
  assert.equal(grams('250'), '250 g');
});

test('minimum stock uses aggregate available quantities and ignores inactive locations', () => {
  const products = [{ productID: 1, minStockThreshold: 10, active: true }, { productID: 2, minStockThreshold: 3, active: true }];
  const stocks = [{ productID: 1, quantity: 8, reserved: 2, active: true }, { productID: 1, quantity: 7, reserved: 1, active: true }, { productID: 2, quantity: 100, reserved: 0, active: false }];
  assert.equal(availableStock(1, stocks), 12);
  assert.equal(availableStock(2, stocks), 0);
  assert.deepEqual(lowStockProducts(products, stocks).map(product => product.productID), [2]);
});
