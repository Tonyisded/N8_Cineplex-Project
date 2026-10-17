import { Module } from "@nestjs/common";
import { CatalogController } from "./catalog.controller.js";
import { ShowtimesController } from "./showtimes.controller.js";

@Module({ controllers: [CatalogController, ShowtimesController] })
export class CatalogModule {}
