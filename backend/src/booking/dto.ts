import {
  ArrayMaxSize,
  ArrayMinSize,
  ArrayUnique,
  IsArray,
  IsUUID,
  IsInt,
  IsOptional,
  IsString,
  Min,
  Max,
  MaxLength,
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

export class QuoteDto {
  @ApiProperty({ format: "uuid", required: false, nullable: true })
  @IsOptional()
  @IsUUID()
  combo_id?: string | null;
  @ApiProperty({ minimum: 0, maximum: 9 })
  @IsInt()
  @Min(0)
  @Max(9)
  combo_quantity!: number;
  @ApiProperty({ required: false })
  @IsOptional()
  @IsString()
  @MaxLength(32)
  promotion_code?: string;
}
export class CheckoutDto {
  @ApiProperty() @IsString() @MaxLength(8192) quoteToken!: string;
}
