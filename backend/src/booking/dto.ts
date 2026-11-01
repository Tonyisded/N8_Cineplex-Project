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
  ValidateIf,
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
  @ApiProperty({
    minimum: 0,
    maximum: 9,
    description:
      "No combo: omit/null combo_id and quantity 0. One complete combo: UUID and integer quantity 1-9. Prices are server-calculated.",
  })
  @IsInt()
  @Min(0)
  @Max(9)
  combo_quantity!: number;
  @ApiProperty({ required: false })
  @IsOptional()
  @IsString()
  @MaxLength(32)
  promotion_code?: string;
  @ApiProperty({
    required: false,
    description:
      "Voucher selection; empty string means no voucher and no stored balance.",
  })
  @ValidateIf((_, value) => value !== undefined)
  @IsString()
  @MaxLength(32)
  voucher_code?: string;
}
export class CheckoutDto {
  @ApiProperty() @IsString() @MaxLength(8192) quoteToken!: string;
}
