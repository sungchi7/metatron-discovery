/*
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

/**
 * symbol layer
 */
import { UILayers } from './ui-layers';
import { MapBy, MapLinePathType, MapSymbolType } from '../../define/map/map-common';

// TODO add extends UILayers later
export interface UILineLayer {
// export interface UILineLayer extends UILayers{

  // Type of Line
  pathType?: MapLinePathType;

  // Source column Name
  source?: string;

  // Target column Name
  target?: string;

  // Thickness of line
  thickness?: UIThickness;
}

/**
 * Thickness of line
 */
// TODO remove export after refactoring
export interface UIThickness {
// interface UIThickness {

  // Color specification criteria
  by?: MapBy;

  // Column Name
  column?: string;

  // Max value of thickness
  maxValue?: number;
}
