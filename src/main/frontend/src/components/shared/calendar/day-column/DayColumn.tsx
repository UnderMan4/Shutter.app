import React, { useMemo } from "react";
import styles from "./DayColumn.module.scss";
import { useSelectable } from "hooks";
import { AvailabilityHour, HourBox, Reservation } from "types/CalendarTypes";
import { AvailabilityBox } from "../availability";
import { ReservationBox } from "../reservation";
import { DateTime } from "luxon";
import { useTranslation } from "react-i18next";

interface Props {
   showWeekNavigation: boolean;
   dayData: HourBox[];
   availabilityList?: AvailabilityHour[];
   reservationsList?: Reservation[];
   isLoading: boolean;
   onRangeSelection?: (selection: HourBox[]) => void;
   onAvailabilityRemove?: (availability: AvailabilityHour) => void;
   onReservationRemove?: (reservation: Reservation) => void;
}

export const DayColumn: React.FC<Props> = ({
   showWeekNavigation,
   dayData,
   availabilityList,
   reservationsList,
   isLoading,
   onRangeSelection,
   onAvailabilityRemove,
   onReservationRemove,
}) => {
   const { i18n } = useTranslation();
   const today = useMemo(() => DateTime.local().startOf("day"), [dayData]);
   const displayFullWidth = !(availabilityList && reservationsList);
   const dayStart = dayData[0].from;

   const labelClass =
      dayStart < today ? styles.before : dayStart.equals(today) && styles.today;

   const Selectable = useSelectable({
      objects: dayData,
      onSelect: (selection) => onRangeSelection(selection),
   });

   return (
      <div className={styles.day_column_wrapper}>
         <div className={`${styles.header} ${labelClass}`}>
            <p className="label-bold">
               {showWeekNavigation
                  ? dayStart.setLocale(i18n.language).toFormat("ccc dd")
                  : dayStart.setLocale(i18n.language).toFormat("ccc")}
            </p>
         </div>
         <div className={styles.content}>
            <div className={styles.grid}>
               {dayData.map((_, index) => {
                  return (
                     <Selectable
                        index={index}
                        disabled={!onRangeSelection}
                        selectedClassName={onRangeSelection ? styles.selected : ""}
                        className={`${styles.half_hour} ${
                           !onRangeSelection ? styles.disabled : ""
                        }`}
                        key={index}
                     />
                  );
               })}
            </div>
            {!isLoading &&
               availabilityList?.map((availability, index) => (
                  <AvailabilityBox
                     availability={availability}
                     key={index}
                     fullWidth={displayFullWidth}
                     onRemove={onAvailabilityRemove}
                  />
               ))}
            {!isLoading &&
               reservationsList?.map((reservation, index) => (
                  <ReservationBox
                     reservation={reservation}
                     key={index}
                     fullWidth={displayFullWidth}
                  />
               ))}
         </div>
      </div>
   );
};
