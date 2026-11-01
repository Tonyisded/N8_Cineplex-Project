import { Module } from "@nestjs/common";
import { CombosController } from "./combos.controller.js";
import { CatalogController } from "./catalog.controller.js";
import { ShowtimesController } from "./showtimes.controller.js";

@Module({
  controllers: [CatalogController, ShowtimesController, CombosController],
})
export class CatalogModule {}
