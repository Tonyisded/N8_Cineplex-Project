import {
  ArrayMaxSize,
  ArrayMinSize,
  ArrayUnique,
  IsArray,
  IsUUID,
} from "class-validator";
import { ApiProperty } from "@nestjs/swagger";

export class CreateOrderDto {
  @ApiProperty({ format: "uuid" }) @IsUUID() showtime_id!: string;
  @ApiProperty({
    type: [String],
    description: "Physical seat UUIDs; every Couple must be complete",
  })
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(496)
  @ArrayUnique()
  @IsUUID(undefined, { each: true })
  seat_ids!: string[];
}
