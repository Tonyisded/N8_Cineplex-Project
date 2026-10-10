import { Transform } from "class-transformer";
import {
  Equals,
  IsEmail,
  IsIn,
  IsOptional,
  IsString,
  Length,
  MaxLength,
} from "class-validator";
import { ApiProperty, ApiPropertyOptional } from "@nestjs/swagger";

const email = () =>
  Transform(({ value }) =>
    typeof value === "string" ? value.trim().toLowerCase() : value,
  );
export class EmailDto {
  @ApiProperty() @email() @IsEmail() @MaxLength(254) email!: string;
}
export class RegisterDto extends EmailDto {
  @ApiProperty()
  @Transform(({ value }) => (typeof value === "string" ? value.trim() : value))
  @IsString()
  @Length(1, 100)
  fullName!: string;
  @ApiProperty({ minLength: 8, maxLength: 128 })
  @IsString()
  @Length(8, 128)
  password!: string;
  @ApiProperty() @Equals(true) consent!: boolean;
}
export class LoginDto extends EmailDto {
  @ApiProperty() @IsString() @Length(1, 128) password!: string;
  @ApiProperty({ enum: ["CUSTOMER", "STAFF"] })
  @IsIn(["CUSTOMER", "STAFF"])
  portal!: "CUSTOMER" | "STAFF";
}
export class TokenDto {
  @ApiProperty() @IsString() @Length(1, 4096) token!: string;
}
export class RefreshDto {
  @ApiProperty() @IsString() @Length(1, 4096) refreshToken!: string;
}
export class GoogleDto {
  @ApiProperty() @IsString() @Length(1, 8192) idToken!: string;
}
export class ResetDto extends TokenDto {
  @ApiProperty() @IsString() @Length(8, 128) newPassword!: string;
}
export class PasswordDto {
  @ApiProperty() @IsString() @Length(1, 128) currentPassword!: string;
  @ApiProperty() @IsString() @Length(8, 128) newPassword!: string;
}
export class ProfileDto {
  @ApiProperty()
  @Transform(({ value }) => (typeof value === "string" ? value.trim() : value))
  @IsString()
  @Length(1, 100)
  fullName!: string;
}
export class ChangeEmailDto {
  @ApiProperty() @email() @IsEmail() @MaxLength(254) newEmail!: string;
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @Length(1, 128)
  currentPassword?: string;
  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @Length(1, 8192)
  idToken?: string;
}
