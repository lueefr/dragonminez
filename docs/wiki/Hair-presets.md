# Saved hair presets

Open the hair editor and select **Saved hair presets**. Enter a name and choose
**Save preset** to save the whole hair set, including base, Super Saiyan variants,
colors, segment settings and the hair-base flag. Selecting a saved entry loads
it into the editor; inspect it and use the editor's normal Save button to apply
it to the current character or editable form. Loading remains subject to the
normal creation, recustomization or Popo permissions of the server.

Presets belong to the client installation, not a world, server, race, character
UUID or body. Reincarnation, reset, body swap and changing servers cannot erase
or mutate this library. They do not automatically overwrite a newly acquired
body's appearance. Use the library to apply a saved style when customization is
available. Saving edited hair creates another entry rather than modifying an
existing preset; deletion asks for confirmation.

Files live in `config/dragonminez/hair_presets/` in the client game directory.
Back up or copy that directory to transfer the library to another installation.
The local library supports 256 named entries, each retaining all hair slots.
Files are versioned and bounded on read; malformed entries are logged and left
untouched. Built-in presets remain unchanged.
