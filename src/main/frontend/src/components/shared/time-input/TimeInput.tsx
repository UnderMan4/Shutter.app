import React, { useMemo } from "react";
import styles from "./TimeInput.module.scss";
import { getHalfHourRange, getHourRange } from "util/calendarUtil";
import { IconDropdown } from "../dropdown";

interface Option {
   [key: string]: string;
}

interface Props {
   className?: string;
   onChange: (time: string) => void;
   value: string;
}

export const TimeInput: React.FC<Props> = ({ className = "", onChange, value }) => {
   const options = useMemo(() => {
      const res: Option = {};
      getHalfHourRange().forEach((halfHour, index) => {
         res[`hour-${index}`] = halfHour;
      });

      return res;
   }, []);

   return (
      <IconDropdown
         className={`${styles.time_input_wrapper} ${className}`}
         value={value ? value : "08:00"}
         onChange={(key) => onChange(options[key])}
         options={options}
      />
   );
};
