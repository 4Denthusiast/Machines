/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.machines.systems;

import com.google.common.collect.Lists;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.machines.components.CategorizedInventoryComponent;
import org.terasology.machines.components.MachineDefinitionComponent;
import org.terasology.machines.components.ProcessRequirementsProviderComponent;
import org.terasology.math.Side;
import org.terasology.workstation.component.WorkstationInventoryComponent;
import org.terasology.world.block.BlockComponent;

import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MachineAuthoritySystem extends BaseComponentSystem {

    @Override
    public void initialise() {
    }

    @ReceiveEvent(components = {LocationComponent.class, BlockComponent.class})
    public void onMachineDefinitionAdded(OnAddedComponent event, EntityRef entity, MachineDefinitionComponent machineDefinition) {
        addProcessingMachine(entity, machineDefinition);
    }

    private void addProcessingMachine(EntityRef entity, MachineDefinitionComponent machineDefinition) {

        // configure the input/output inventories
        if (!entity.hasComponent(InventoryComponent.class)) {
            int totalSlots = machineDefinition.inputSlots + machineDefinition.requirementSlots + machineDefinition.outputSlots;
            InventoryComponent inventoryComponent = new InventoryComponent(totalSlots);
            inventoryComponent.privateToOwner = false;
            entity.addComponent(inventoryComponent);
        }

        // configure the workstation inventory
        if (!entity.hasComponent(WorkstationInventoryComponent.class)) {
            WorkstationInventoryComponent workstationInventory = new WorkstationInventoryComponent();
            int totalInputSlots = machineDefinition.inputSlots + machineDefinition.requirementSlots;
            workstationInventory.slotAssignments.put("INPUT", new WorkstationInventoryComponent.SlotAssignment(0, machineDefinition.inputSlots));
            workstationInventory.slotAssignments.put("OUTPUT", new WorkstationInventoryComponent.SlotAssignment(totalInputSlots, machineDefinition.outputSlots));
            entity.addComponent(workstationInventory);
        }

        // configure the categorized inventory
        if (!entity.hasComponent(CategorizedInventoryComponent.class)) {
            CategorizedInventoryComponent categorizedInventory = new CategorizedInventoryComponent();
            int totalInputSlots = machineDefinition.inputSlots + machineDefinition.requirementSlots;
            categorizedInventory.slotMapping.put("INPUT",
                    createSlotRange(0, machineDefinition.inputSlots));
            categorizedInventory.slotMapping.put("REQUIREMENTS",
                    createSlotRange(machineDefinition.inputSlots, machineDefinition.requirementSlots));
            categorizedInventory.slotMapping.put("OUTPUT",
                    createSlotRange(totalInputSlots, machineDefinition.outputSlots));

            // add default input
            categorizedInventory.slotMapping.put(Side.TOP.toString(), categorizedInventory.slotMapping.get("INPUT"));

            // add default output
            for (Side side : Side.horizontalSides()) {
                categorizedInventory.slotMapping.put(side.toString(), categorizedInventory.slotMapping.get("OUTPUT"));
            }

            entity.addComponent(categorizedInventory);
        }

        // add the requirements provider to the input entity
        entity.addComponent(new ProcessRequirementsProviderComponent(machineDefinition.requirementsProvided.toArray(new String[0])));
    }

    List<Integer> createSlotRange(int startIndex, int length) {
        List<Integer> slots = Lists.newArrayList();
        for (int i = 0; i < length; i++) {
            slots.add(startIndex + i);
        }

        return slots;
    }
}
