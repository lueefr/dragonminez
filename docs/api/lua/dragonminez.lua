-- Generated DragonMineZ Lua editor declarations. Do not edit.
---@meta

---@alias DMZEvent string
---| 'dmz:tick'
---| 'dmz:form_changed'
---| 'dmz:combat_enter'
---| 'dmz:combat_exit'
---| 'dmz:damage_received'
---| 'dmz:damage_dealt'
---| 'dmz:evaded'
---| 'dmz:blocked'
---| 'dmz:countered'
---| 'dmz:attack_started'
---| 'dmz:attack_released'
---| 'dmz:attack_cancelled'
---| 'dmz:attack_recovery'
---| 'dmz:projectile_destroyed'
---| 'dmz:projectile_deflected'
---| 'dmz:attack_finished'

---@alias DMZSource string
---| 'state.formTicks'
---| 'state.stackTicks'
---| 'self.healthPercent'
---| 'attack.charging'
---| 'attack.active'
---| 'attack.recovery'
---| 'attack.ticks'
---| 'attack.charge'
---| 'event.damage'
---| 'event.originalDamage'
---| 'event.blockedDamage'

---Pre-event snapshot. Keys meter:<namespace:path> contain declared meters.
---Damage sources exist only on damage/block/counter events.
---@class DMZContext
---@field event DMZEvent
---@field values table<DMZSource, number>
---@field flags table<string, boolean> Named JSON queries evaluated before event actions.

---@class DMZMeter
---@field meter string
---@field operation 'add'|'set'
---@field value number

---Uses existing JSON reward schemas; never a direct Java object.
---@class DMZReward
---@field reward table<string, any>

---@class DMZContent
---@field type 'form_group'|'stack_group'|'attribute'
---@field id string
---@field race? string
---@field mode? 'add'|'replace'
---@field value table<string, any>

---@class DMZModule
---@field content? DMZContent[]
---@field onEvent? fun(ctx: DMZContext): (DMZMeter|DMZReward)[]?

---@type DMZModule
local module = {}
return module
