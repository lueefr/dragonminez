# Clothing and the female body top

Edit `config/dragonminez/appearance/clothing.json` on the server, then restart it. Clothing rules and the gamerule are synchronized to clients.

`/gamerule dmzClothingHidesTop true` enables clothing rules. The default file includes
35 existing open-torso outfits. Rules match item IDs or tags, optional `minDamage` /
`maxDamage` fractions, and `hideTop`; the last matching rule wins. For example:

```json
{ "rules": [
  { "items": ["dragonminez:gogeta_armor_chestplate"], "hideTop": true },
  { "tags": ["example:open_torso"], "minDamage": 0.5, "hideTop": true }
] }
```

Visible cosmetic armor is used. The body top and its shadow are removed; lower underwear
is retained. The texture masks follow the mod's existing female body UV layout.



Each rule needs at least one `items` or `tags` entry. IDs use `namespace:path`;
tags do not include a leading `#`. Matching an item or a tag is sufficient.
`minDamage` and `maxDamage` are inclusive fractions from `0` (undamaged) to `1`
(fully damaged), and default to `0` and `1`. `hideTop` defaults to `true`.

To override a broad tag rule for one item, put the exception last:

```json
{
  "rules": [
    { "tags": ["example:open_torso"], "hideTop": true },
    { "items": ["example:closed_kimono"], "hideTop": false }
  ]
}
```

`/gamerule dmzClothingHidesTop false` disables this behavior without deleting the rules.
Only the body top/shadow is affected; this does not change the clothing item's armor
stats or its own texture. Custom outfits need compatible body UVs to use the same mask.
Invalid files remain untouched and the bundled clothing defaults are used with an error
in the server log.
